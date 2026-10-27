package ma.noba.queue;

import ma.noba.queue.application.service.QueueApplicationService;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueueStatus;
import ma.noba.queue.domain.port.service.BusinessHoursPort;
import ma.noba.queue.domain.port.service.QueueMemberRepository;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.queue.domain.port.service.ServiceDurationEstimator;
import ma.noba.queue.infra.QueueScheduler;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.stubbing.Answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueApplicationServiceTest {

    @Mock QueueRepository queueRepository;
    @Mock QueueMemberRepository memberRepository;
    @Mock ServiceDurationEstimator durationEstimator;
    @Mock QueueNotificationPort notificationPort;
    @Mock EventBus eventBus;
    @Mock BusinessHoursPort businessHoursPort;
    @Mock ma.noba.queue.domain.port.service.ProviderQueuePolicyPort queuePolicy;
    @Mock QueueScheduler queueScheduler;
    @Mock ma.noba.queue.domain.port.service.ClientDisplayNamePort clientDisplayNamePort;
    @Mock ma.noba.queue.application.service.QueueWebSocketService queueWebSocketService;
    @Mock ma.noba.queue.application.service.QueueWaitlistApplicationService waitlistService;
    @Mock ma.noba.queue.domain.port.service.ProviderServiceNamePort serviceNamePort;

    private QueueApplicationService service;
    private UUID providerId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(queuePolicy.policy(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ma.noba.queue.domain.port.service.ProviderQueuePolicyPort.QueuePolicy(10, true));
        service = new QueueApplicationService(queueRepository, memberRepository,
                durationEstimator, notificationPort, eventBus, businessHoursPort, queuePolicy, queueScheduler,
                clientDisplayNamePort, serviceNamePort, queueWebSocketService, waitlistService);
        providerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
    }

    private Queue createQueue() {
        var q = Queue.create(providerId);
        q.setId(UUID.randomUUID());
        return q;
    }

    private QueueMember addMember(Queue queue, UUID clientId) {
        var member = queue.addMember(clientId, Duration.ofMinutes(15), null);
        member.setId(UUID.randomUUID());
        return member;
    }

    private Queue assignMemberIds(Queue q) {
        q.getActiveMembers().forEach(m -> { if (m.getId() == null) m.setId(UUID.randomUUID()); });
        return q;
    }

    private Answer<Queue> withIds() {
        return i -> assignMemberIds((Queue) i.getArgument(0));
    }

    @Test
    @DisplayName("Join queue — creates new queue if none exists")
    void joinQueue_createsNewQueue() {
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(true);
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(durationEstimator.getDefaultDuration()).thenReturn(Duration.ofMinutes(15));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> {
            var q = (Queue) i.getArgument(0);
            q.setId(UUID.randomUUID());
            return assignMemberIds(q);
        });

        var result = service.joinQueue(providerId, clientId, null);

        assertThat(result).isNotNull();
        assertThat(result.position()).isEqualTo(1);
        verify(eventBus).publish(any());
    }

    @Test
    @DisplayName("Join queue — adds to existing queue")
    void joinQueue_existingQueue() {
        var queue = createQueue();
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(true);
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(durationEstimator.getDefaultDuration()).thenReturn(Duration.ofMinutes(15));
        when(queueRepository.save(any(Queue.class))).thenAnswer(withIds());

        var result = service.joinQueue(providerId, clientId, null);

        assertThat(result.position()).isEqualTo(1);
        verify(eventBus).publish(any());
    }

    @Test
    @DisplayName("Join queue — when provider is closed throws QUEUE_CLOSED")
    void joinQueue_closed() {
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(false);

        assertThatThrownBy(() -> service.joinQueue(providerId, clientId, null))
                .isInstanceOf(ValidationException.class)
                .matches(e -> ((ValidationException) e).getErrorCode() == ErrorCode.QUEUE_CLOSED);

        verify(queueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Join queue — uses service duration when serviceId provided")
    void joinQueue_withServiceDuration() {
        var serviceId = "service-1";
        var queue = createQueue();
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(true);
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(durationEstimator.estimate(providerId, serviceId)).thenReturn(Duration.ofMinutes(30));
        when(queueRepository.save(any(Queue.class))).thenAnswer(withIds());

        service.joinQueue(providerId, clientId, serviceId);

        verify(durationEstimator).estimate(providerId, serviceId);
        verify(durationEstimator, never()).getDefaultDuration();
    }

    @Test
    @DisplayName("Leave queue — removes member and reschedules")
    void leaveQueue_success() {
        var queue = createQueue();
        addMember(queue, clientId);

        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));

        service.leaveQueue(providerId, clientId);

        verify(eventBus).publish(any());
    }

    @Test
    @DisplayName("Leave queue — when not in queue throws NotFoundException")
    void leaveQueue_notInQueue() {
        var queue = createQueue();
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));

        assertThatThrownBy(() -> service.leaveQueue(providerId, UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Advance queue — completes active, activates next")
    void advanceQueue_success() {
        var queue = createQueue();
        addMember(queue, clientId);
        addMember(queue, UUID.randomUUID());

        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));

        service.advanceQueue(providerId);

        assertThat(queue.getActiveMembers().stream().filter(m -> m.getStatus() == QueueMemberStatus.ACTIVE).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mark no-show — removes the member and promotes the next")
    void markNoShowPromotesNext() {
        var queue = createQueue();
        var first = addMember(queue, clientId);
        var second = addMember(queue, UUID.randomUUID());
        queue.advanceToNext(); // first becomes ACTIVE (serving)

        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenReturn(queue);

        service.markNoShow(providerId, first.getId());

        assertThat(queue.getActiveMembers()).extracting(QueueMember::getId).contains(second.getId());
        assertThat(queue.getActiveMembers()).extracting(QueueMember::getStatus).contains(QueueMemberStatus.ACTIVE);
        verify(eventBus, atLeastOnce()).publish(any());
    }

    @Test
    @DisplayName("Pause queue — sets status to PAUSED")
    void pauseQueue_success() {
        var queue = createQueue();
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));

        service.pauseQueue(providerId);

        assertThat(queue.getStatus()).isEqualTo(QueueStatus.PAUSED);
    }

    @Test
    @DisplayName("Resume queue — sets status to ACTIVE")
    void resumeQueue_success() {
        var queue = createQueue();
        queue.pause();
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));

        service.resumeQueue(providerId);

        assertThat(queue.getStatus()).isEqualTo(QueueStatus.ACTIVE);
    }

    @Test
    @DisplayName("Close queue — sets status to CLOSED")
    void closeQueue_success() {
        var queue = createQueue();
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(queueRepository.save(any(Queue.class))).thenAnswer(i -> i.getArgument(0));

        service.closeQueue(providerId);

        assertThat(queue.getStatus()).isEqualTo(QueueStatus.CLOSED);
    }

    @Test
    @DisplayName("Queue members expose the client display name (not a raw id)")
    void queueMembersIncludeClientName() {
        var queue = createQueue();
        addMember(queue, clientId);
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));
        when(clientDisplayNamePort.findDisplayNames(any())).thenReturn(java.util.Map.of(clientId, "Amine"));

        var result = service.getQueueMembers(providerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).clientName()).isEqualTo("Amine");
    }

    @Test
    @DisplayName("Get queue status — returns status summary")
    void getQueueStatus_success() {
        var queue = createQueue();
        addMember(queue, clientId);
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.of(queue));

        var result = service.getQueueStatus(providerId);

        assertThat(result.status()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(result.waitingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get queue status — queue not found throws exception")
    void getQueueStatus_notFound() {
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQueueStatus(providerId))
                .isInstanceOf(NotFoundException.class)
                .matches(e -> ((NotFoundException) e).getErrorCode() == ErrorCode.QUEUE_NOT_FOUND);
    }

    @Test
    @DisplayName("Join is rejected when the party exceeds the provider's max party size")
    void joinQueue_rejectsPartyOverMax() {
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(true);
        when(queuePolicy.policy(providerId))
                .thenReturn(new ma.noba.queue.domain.port.service.ProviderQueuePolicyPort.QueuePolicy(2, true));

        assertThatThrownBy(() -> service.joinQueue(providerId, clientId, null, null, null, 5))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Join is rejected when walk-ins are disabled for the provider")
    void joinQueue_rejectsWhenWalkInsDisabled() {
        when(businessHoursPort.isOpenNow(providerId)).thenReturn(true);
        when(queuePolicy.policy(providerId))
                .thenReturn(new ma.noba.queue.domain.port.service.ProviderQueuePolicyPort.QueuePolicy(10, false));

        assertThatThrownBy(() -> service.joinQueue(providerId, clientId, null, null, null, 1))
                .isInstanceOf(ValidationException.class);
    }
}
