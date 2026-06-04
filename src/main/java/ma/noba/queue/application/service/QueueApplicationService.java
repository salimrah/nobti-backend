package ma.noba.queue.application.service;

import ma.noba.queue.application.dto.JoinQueueResponse;
import ma.noba.queue.application.dto.QueueMemberResponse;
import ma.noba.queue.application.dto.QueueStatusResponse;
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
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.queue.domain.port.service.QueueSchedulingPort;
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
import java.util.List;
import java.util.UUID;

@Service
public class QueueApplicationService {

    private final QueueRepository queueRepository;
    private final QueueMemberRepository memberRepository;
    private final ServiceDurationEstimator durationEstimator;
    private final QueueNotificationPort notificationPort;
    private final EventBus eventBus;
    private final BusinessHoursPort businessHoursPort;
    private final QueueSchedulingPort queueSchedulingPort;

    public QueueApplicationService(
            QueueRepository queueRepository,
            QueueMemberRepository memberRepository,
            ServiceDurationEstimator durationEstimator,
            QueueNotificationPort notificationPort,
            EventBus eventBus,
            BusinessHoursPort businessHoursPort,
            QueueSchedulingPort queueSchedulingPort) {
        this.queueRepository = queueRepository;
        this.memberRepository = memberRepository;
        this.durationEstimator = durationEstimator;
        this.notificationPort = notificationPort;
        this.eventBus = eventBus;
        this.businessHoursPort = businessHoursPort;
        this.queueSchedulingPort = queueSchedulingPort;
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

        Queue queue = getOrCreateQueue(providerId);

        Duration duration = serviceId != null
                ? durationEstimator.estimate(providerId, serviceId)
                : durationEstimator.getDefaultDuration();

        QueuePriority effectivePriority = priority != null ? priority : QueuePriority.NORMAL;
        int size = groupSize != null ? groupSize : 1;
        QueueMember member = queue.addMember(clientId, duration, serviceId, employeeId, groupId, size, effectivePriority);
        queueRepository.save(queue);

        int waitMinutes = (int) Duration.between(Instant.now(), member.getEstimatedStartTime()).toMinutes();

        eventBus.publish(new ClientJoinedQueueEvent(providerId, clientId, queue.getId(), member.getPosition(), member.getEstimatedStartTime()));

        return new JoinQueueResponse(
                queue.getId().toString(),
                member.getId().toString(),
                member.getPosition(),
                effectivePriority,
                member.getEstimatedStartTime(),
                waitMinutes
        );
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void leaveQueue(UUID providerId, UUID clientId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember member = queue.findMemberByClient(clientId)
                .orElseThrow(() -> new NotFoundException("Queue member not found", ""));

        queue.removeMember(member.getId());
        queueRepository.save(queue);

        queue.getActiveMembers().stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING)
                .forEach(m -> queueSchedulingPort.rescheduleTurnReminder(
                        m.getClientId(), providerId, m.getPosition(), m.getEstimatedStartTime()));

        eventBus.publish(new ClientLeftQueueEvent(providerId, clientId, queue.getId()));
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
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void resumeQueue(UUID providerId) {
        Queue queue = getOrCreateQueue(providerId);
        if (queue.getStatus() == QueueStatus.PAUSED) {
            queue.resume();
        }
        queueRepository.save(queue);
    }

    @Transactional
    @CacheEvict(value = "queueStatus", key = "#providerId")
    public void closeQueue(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));
        queue.close();
        queueRepository.save(queue);
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

        return new QueueStatusResponse(
                queue.getId().toString(),
                queue.getStatus(),
                waitingCount,
                activeCount,
                nextEstimated
        );
    }

    @Transactional(readOnly = true)
    public List<QueueMemberResponse> getQueueMembers(UUID providerId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        return queue.getActiveMembers().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<QueueMemberResponse> getQueueMembers(UUID providerId, Pageable pageable) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        Page<QueueMemberResponse> page = memberRepository.findByQueueId(queue.getId(), pageable)
                .map(this::toResponse);
        return PagedResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public QueueMemberResponse getMyQueuePosition(UUID providerId, UUID clientId) {
        Queue queue = queueRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active queue for provider"));

        QueueMember member = queue.findMemberByClient(clientId)
                .orElseThrow(() -> new NotFoundException("Not in queue", ""));

        return toResponse(member);
    }

    @Transactional(readOnly = true)
    public List<QueueMemberResponse> getMyQueuesToday(UUID clientId) {
        return memberRepository.findByClientId(clientId).stream()
                .filter(m -> m.getCreatedAt().isAfter(Instant.now().minus(Duration.ofDays(1))))
                .map(this::toResponse)
                .toList();
    }

    private QueueMemberResponse toResponse(QueueMember member) {
        int waitMinutes = (int) Duration.between(Instant.now(), member.getEstimatedStartTime()).toMinutes();
        double actualDuration = member.getActualDuration() != null
                ? member.getActualDuration().toMinutes()
                : 0.0;
        return new QueueMemberResponse(
                member.getId().toString(),
                member.getClientId().toString(),
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
                member.getGroupSize()
        );
    }
}
