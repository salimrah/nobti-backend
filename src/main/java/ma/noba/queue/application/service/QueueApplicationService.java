package ma.noba.queue.application.service;

import ma.noba.queue.application.dto.JoinQueueResponse;
import ma.noba.queue.application.dto.QueueMemberResponse;
import ma.noba.queue.application.dto.QueueStatusResponse;
import ma.noba.queue.application.dto.WaitEstimate;
import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.event.ClientServiceCompletedEvent;
import ma.noba.queue.domain.event.ClientTurnStartedEvent;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueuePriority;
import ma.noba.queue.domain.model.QueueStatus;
import ma.noba.queue.domain.port.service.BusinessHoursPort;
import ma.noba.queue.domain.port.service.ClientDisplayNamePort;
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.queue.domain.port.service.QueueSchedulingPort;
import ma.noba.queue.domain.port.service.ProviderQueuePolicyPort;
import ma.noba.queue.domain.port.service.ServiceDurationEstimator;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QueueApplicationService {

    private final QueueRepository queueRepository;
    private final QueueMemberRepository memberRepository;
    private final ServiceDurationEstimator durationEstimator;
    private final QueueNotificationPort notificationPort;
    private final EventBus eventBus;
    private final BusinessHoursPort businessHoursPort;
    private final ProviderQueuePolicyPort queuePolicy;
    private final QueueSchedulingPort queueSchedulingPort;
    private final ClientDisplayNamePort clientDisplayNamePort;
    private final QueueWebSocketService queueWebSocketService;
    private final QueueWaitlistApplicationService waitlistService;

    public QueueApplicationService(
            QueueRepository queueRepository,
            QueueMemberRepository memberRepository,
            ServiceDurationEstimator durationEstimator,
            QueueNotificationPort notificationPort,
            EventBus eventBus,
            BusinessHoursPort businessHoursPort,
            ProviderQueuePolicyPort queuePolicy,
            QueueSchedulingPort queueSchedulingPort,
            ClientDisplayNamePort clientDisplayNamePort,
            QueueWebSocketService queueWebSocketService,
            QueueWaitlistApplicationService waitlistService) {
        this.queueRepository = queueRepository;
        this.memberRepository = memberRepository;
        this.durationEstimator = durationEstimator;
        this.notificationPort = notificationPort;
        this.eventBus = eventBus;
        this.businessHoursPort = businessHoursPort;
        this.queuePolicy = queuePolicy;
        this.queueSchedulingPort = queueSchedulingPort;
        this.clientDisplayNamePort = clientDisplayNamePort;
        this.queueWebSocketService = queueWebSocketService;
        this.waitlistService = waitlistService;
    }

    @Transactional
    public Queue getOrCreateQueue(UUID providerId) {
        return queueRepository.findByProviderId(providerId)
                .map(queue -> {
                    if (!businessHoursPort.isOpenNow(providerId) && queue.getStatus() == QueueStatus.ACTIVE) {
                        queue.close();
                        queueRepository.save(queue);
                    }
                    return queue;
                })
                .orElseGet(() -> queueRepository.save(Queue.create(providerId)));
    }

    public JoinQueueResponse joinQueue(UUID providerId, UUID clientId, String serviceId) {
        return joinQueue(providerId, clientId, serviceId, null, null, 1, null);
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public JoinQueueResponse joinQueue(UUID providerId, UUID clientId, String serviceId, UUID employeeId) {
        return joinQueue(providerId, clientId, serviceId, employeeId, null, 1, null);
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public JoinQueueResponse joinQueue(UUID providerId, UUID clientId, String serviceId, UUID employeeId, UUID groupId, Integer groupSize) {
        return joinQueue(providerId, clientId, serviceId, employeeId, groupId, groupSize, null);
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public JoinQueueResponse joinQueue(UUID providerId, UUID clientId, String serviceId, UUID employeeId, UUID groupId, Integer groupSize, QueuePriority priority) {
        if (!businessHoursPort.isOpenNow(providerId)) {
            throw new ValidationException(ErrorCode.QUEUE_CLOSED, "Provider is currently closed");
        }

        ProviderQueuePolicyPort.QueuePolicy policy = queuePolicy.policy(providerId);
        if (!policy.walkInsEnabled()) {
            throw new ValidationException(ErrorCode.QUEUE_CLOSED, "Walk-ins are currently disabled");
        }

        int size = groupSize != null ? groupSize : 1;
        if (size > policy.maxPartySize()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "Party size exceeds the maximum of " + policy.maxPartySize());
        }

        Queue queue = getOrCreateQueue(providerId);

        Duration duration = serviceId != null
                ? durationEstimator.estimate(providerId, serviceId)
                : durationEstimator.getDefaultDuration();

        QueuePriority effectivePriority = priority != null ? priority : QueuePriority.NORMAL;
        QueueMember member = queue.addMember(clientId, duration, serviceId, employeeId, groupId, size, effectivePriority);
        // Persist the member explicitly so its generated identifier is assigned (on this instance)
        // before the response is built. Relying on the queue cascade left member.getId() null here.
        memberRepository.save(member);
        queueRepository.save(queue);

        // If the client was on the waitlist, they've now converted to a real queue member.
        waitlistService.markConverted(providerId, clientId);

        int waitMinutes = (int) Duration.between(Instant.now(), member.getEstimatedStartTime()).toMinutes();

        eventBus.publish(new ClientJoinedQueueEvent(providerId, clientId, queue.getId(), member.getPosition(), member.getEstimatedStartTime()));

        WaitEstimate estimate = WaitEstimateCalculator.forMember(
                Math.max(0, waitMinutes), completedSampleSize(queue), Math.max(0, member.getPosition() - 1));

        return new JoinQueueResponse(
                queue.getId().toString(),
                member.getId().toString(),
                member.getPosition(),
                effectivePriority,
                member.getEstimatedStartTime(),
                waitMinutes,
                estimate
        );
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void leaveQueue(UUID providerId, UUID clientId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember member = queue.findMemberByClient(clientId)
                .orElseThrow(() -> new NotFoundException("Queue member not found", ""));

        Optional<QueueMember> promoted = queue.removeMember(member.getId());
        queueRepository.save(queue);

        queue.getActiveMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                .forEach(m -> queueSchedulingPort.rescheduleTurnReminder(
                        m.getClientId(), providerId, m.getPosition(), m.getEstimatedStartTime()));

        eventBus.publish(new ClientLeftQueueEvent(providerId, clientId, queue.getId()));
        // Auto-fill: if the client who left was being served, notify whoever was promoted.
        promoted.ifPresent(p -> eventBus.publish(
                new ClientTurnStartedEvent(providerId, p.getClientId(), queue.getId())));
    }

    /**
     * Provider marks a waiting client as a no-show (they never turned up): the client is removed and
     * the next waiting client is automatically promoted and notified.
     */
    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void markNoShow(UUID providerId, UUID memberId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember member = queue.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Queue member not found", ""));
        UUID clientId = member.getClientId();

        Optional<QueueMember> promoted = queue.removeMember(memberId);
        queueRepository.save(queue);

        eventBus.publish(new ClientLeftQueueEvent(providerId, clientId, queue.getId()));
        promoted.ifPresent(p -> eventBus.publish(
                new ClientTurnStartedEvent(providerId, p.getClientId(), queue.getId())));
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void advanceQueue(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember completed = queue.getMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        queue.advanceToNext();
        queueRepository.save(queue);

        if (completed != null) {
            eventBus.publish(new ClientServiceCompletedEvent(
                    providerId, completed.getClientId(), queue.getId(), completed.getServiceId()));
        }

        queue.getActiveMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                .forEach(m -> queueSchedulingPort.rescheduleTurnReminder(
                        m.getClientId(), providerId, m.getPosition(), m.getEstimatedStartTime()));

        QueueMember next = queue.getActiveMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        if (next != null) {
            eventBus.publish(new ClientTurnStartedEvent(providerId, next.getClientId(), queue.getId()));
        }
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void pauseQueue(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));
        queue.pause();
        queueRepository.save(queue);
        queueWebSocketService.broadcastQueueStatus(providerId, queue.getStatus());
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void resumeQueue(UUID providerId) {
        Queue queue = getOrCreateQueue(providerId);
        // A closed queue must be recoverable, otherwise providers cannot reopen for the day.
        if (queue.getStatus() == QueueStatus.PAUSED || queue.getStatus() == QueueStatus.CLOSED) {
            queue.resume();
        }
        queueRepository.save(queue);
        queueWebSocketService.broadcastQueueStatus(providerId, queue.getStatus());
        // Opening the queue is the moment to notify everyone waiting on the waitlist.
        waitlistService.notifyWaitlist(providerId, 20);
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void closeQueue(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));
        queue.close();
        queueRepository.save(queue);
        queueWebSocketService.broadcastQueueStatus(providerId, queue.getStatus());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "queueStatus", key = "#providerId")
    public QueueStatusResponse getQueueStatus(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        var activeMembers = queue.getActiveMembers();
        int waitingCount = (int) activeMembers.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING || m.getStatus() == QueueMemberStatus.NOTIFIED)
                .count();
        int activeCount = (int) activeMembers.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.ACTIVE)
                .count();

        Instant nextEstimated = activeMembers.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                .findFirst()
                .map(QueueMember::getEstimatedStartTime)
                .orElse(null);

        WaitEstimate nextEstimate = activeMembers.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                .findFirst()
                .map(m -> WaitEstimateCalculator.forMember(
                        Math.max(0, (int) Duration.between(Instant.now(), m.getEstimatedStartTime()).toMinutes()),
                        completedSampleSize(queue),
                        Math.max(0, m.getPosition() - 1)))
                .orElse(null);

        return new QueueStatusResponse(
                queue.getId().toString(),
                queue.getStatus(),
                waitingCount,
                activeCount,
                nextEstimated,
                nextEstimate
        );
    }

    @Transactional(readOnly = true)
    public List<QueueMemberResponse> getQueueMembers(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        List<QueueMember> active = queue.getActiveMembers();
        Map<UUID, String> names = clientNames(active);
        return active.stream()
                .map(m -> toResponse(m, names.get(m.getClientId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<QueueMemberResponse> getQueueMembers(UUID providerId, Pageable pageable) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        // Only active members (waiting/notified/serving) belong in the live queue view.
        Page<QueueMember> page = memberRepository.findByQueueIdAndStatusIn(
                queue.getId(),
                List.of(QueueMemberStatus.WAITING, QueueMemberStatus.NOTIFIED, QueueMemberStatus.ACTIVE),
                pageable);
        Map<UUID, String> names = clientNames(page.getContent());
        Page<QueueMemberResponse> mapped = page.map(m -> toResponse(m, names.get(m.getClientId())));
        return PagedResponse.of(mapped.getContent(), mapped.getNumber(), mapped.getSize(), mapped.getTotalElements());
    }

    @Transactional(readOnly = true)
    public QueueMemberResponse getMyQueuePosition(UUID providerId, UUID clientId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember member = queue.findMemberByClient(clientId)
                .orElseThrow(() -> new NotFoundException("Not in queue", ""));

        return toResponse(member, clientDisplayNamePort.findDisplayNames(Set.of(clientId)).get(clientId));
    }

    @Transactional(readOnly = true)
    public List<QueueMemberResponse> getMyQueuesToday(UUID clientId) {
        List<QueueMember> members = memberRepository.findByClientId(clientId).stream()
                .filter(m -> m.getCreatedAt().isAfter(Instant.now().minus(Duration.ofDays(1))))
                .toList();
        Map<UUID, String> names = clientNames(members);
        return members.stream()
                .map(m -> toResponse(m, names.get(m.getClientId())))
                .toList();
    }

    private Map<UUID, String> clientNames(Collection<QueueMember> members) {
        Set<UUID> ids = members.stream().map(QueueMember::getClientId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clientDisplayNamePort.findDisplayNames(ids);
    }

    private QueueMemberResponse toResponse(QueueMember member, String clientName) {
        int waitMinutes = (int) Duration.between(Instant.now(), member.getEstimatedStartTime()).toMinutes();
        double actualDuration = member.getActualDuration() != null
                ? member.getActualDuration().toMinutes()
                : 0.0;
        Queue queue = member.getQueue();
        String providerId = queue != null ? queue.getProviderId().toString() : null;
        int queueSize = queue != null ? queue.getActiveMembers().size() : 0;
        WaitEstimate estimate = WaitEstimateCalculator.forMember(
                Math.max(0, waitMinutes),
                queue != null ? completedSampleSize(queue) : 0,
                Math.max(0, member.getPosition() - 1));
        return new QueueMemberResponse(
                member.getId().toString(),
                member.getClientId().toString(),
                clientName,
                member.getServiceId(),
                member.getEmployeeId() != null ? member.getEmployeeId().toString() : null,
                member.getPosition(),
                member.getStatus(),
                member.getPriority(),
                member.getEstimatedStartTime(),
                member.getActualStartTime(),
                actualDuration,
                Math.max(0, waitMinutes),
                member.getGroupId() != null ? member.getGroupId().toString() : null,
                member.getGroupSize(),
                providerId,
                queueSize,
                estimate
        );
    }

    /**
     * How many completed visits with a measured duration the provider has accumulated — the history
     * sample that drives the confidence level of the wait estimate.
     */
    private int completedSampleSize(Queue queue) {
        if (queue == null || queue.getMembers() == null) {
            return 0;
        }
        return (int) queue.getMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.COMPLETED && m.getActualDuration() != null)
                .count();
    }
}
