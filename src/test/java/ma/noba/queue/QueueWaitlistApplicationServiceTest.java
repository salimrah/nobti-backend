package ma.noba.queue;

import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.queue.application.service.QueueWaitlistApplicationService;
import ma.noba.queue.domain.model.QueueWaitlistEntry;
import ma.noba.queue.domain.model.QueueWaitlistStatus;
import ma.noba.queue.domain.port.service.ClientDisplayNamePort;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.queue.domain.port.service.QueueWaitlistRepository;
import ma.noba.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueWaitlistApplicationServiceTest {

    @Mock QueueWaitlistRepository waitlistRepository;
    @Mock QueueRepository queueRepository;
    @Mock QueueNotificationPort notificationPort;
    @Mock ProviderLookupPort providerLookupPort;
    @Mock ClientDisplayNamePort clientDisplayNamePort;

    private QueueWaitlistApplicationService service;

    private final UUID providerId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QueueWaitlistApplicationService(
                waitlistRepository, queueRepository, notificationPort, providerLookupPort, clientDisplayNamePort);
    }

    private QueueWaitlistEntry savedEntry(UUID forClient) {
        QueueWaitlistEntry entry = new QueueWaitlistEntry(providerId, forClient);
        entry.setId(UUID.randomUUID());
        return entry;
    }

    @Test
    @DisplayName("Joining the waitlist persists a WAITING entry")
    void joinCreatesEntry() {
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(waitlistRepository.findActiveByProviderAndClient(providerId, clientId)).thenReturn(Optional.empty());
        when(waitlistRepository.save(any())).thenAnswer(inv -> {
            QueueWaitlistEntry e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        var response = service.joinWaitlist(providerId, clientId);

        assertThat(response.status()).isEqualTo("WAITING");
        assertThat(response.providerId()).isEqualTo(providerId.toString());
        verify(waitlistRepository).save(any());
    }

    @Test
    @DisplayName("Joining twice is rejected")
    void joinTwiceRejected() {
        when(queueRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(waitlistRepository.findActiveByProviderAndClient(providerId, clientId))
                .thenReturn(Optional.of(savedEntry(clientId)));

        assertThatThrownBy(() -> service.joinWaitlist(providerId, clientId))
                .isInstanceOf(ValidationException.class);
        verify(waitlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Leaving the waitlist cancels the entry")
    void leaveCancels() {
        QueueWaitlistEntry entry = savedEntry(clientId);
        when(waitlistRepository.findActiveByProviderAndClient(providerId, clientId)).thenReturn(Optional.of(entry));

        service.leaveWaitlist(providerId, clientId);

        assertThat(entry.getStatus()).isEqualTo(QueueWaitlistStatus.CANCELLED);
        verify(waitlistRepository).save(entry);
    }

    @Test
    @DisplayName("Notifying marks the oldest WAITING entries NOTIFIED and pushes to each")
    void notifyMarksAndPushes() {
        QueueWaitlistEntry first = savedEntry(UUID.randomUUID());
        QueueWaitlistEntry second = savedEntry(UUID.randomUUID());
        when(waitlistRepository.findActiveByProvider(providerId)).thenReturn(List.of(first, second));
        when(waitlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int notified = service.notifyWaitlist(providerId, 1);

        assertThat(notified).isEqualTo(1);
        assertThat(first.getStatus()).isEqualTo(QueueWaitlistStatus.NOTIFIED);
        assertThat(first.getNotifiedAt()).isNotNull();
        assertThat(second.getStatus()).isEqualTo(QueueWaitlistStatus.WAITING);
        verify(notificationPort).notifyWaitlistOpen(first.getClientId(), providerId);
        verify(notificationPort, never()).notifyWaitlistOpen(second.getClientId(), providerId);
    }

    @Test
    @DisplayName("Converting an active entry marks it CONVERTED")
    void markConverted() {
        QueueWaitlistEntry entry = savedEntry(clientId);
        when(waitlistRepository.findActiveByProviderAndClient(providerId, clientId)).thenReturn(Optional.of(entry));

        service.markConverted(providerId, clientId);

        assertThat(entry.getStatus()).isEqualTo(QueueWaitlistStatus.CONVERTED);
    }

    @Test
    @DisplayName("Provider waitlist is returned FIFO with client names and positions")
    void providerWaitlist() {
        QueueWaitlistEntry first = savedEntry(UUID.randomUUID());
        QueueWaitlistEntry second = savedEntry(UUID.randomUUID());
        when(waitlistRepository.findActiveByProvider(providerId)).thenReturn(List.of(first, second));
        when(clientDisplayNamePort.findDisplayNames(any(Set.class)))
                .thenReturn(Map.of(first.getClientId(), "Amine"));
        when(providerLookupPort.findProviderName(providerId)).thenReturn(Optional.of("Barber Atlas"));

        var list = service.getProviderWaitlist(providerId);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).position()).isEqualTo(1);
        assertThat(list.get(0).clientName()).isEqualTo("Amine");
        assertThat(list.get(0).providerName()).isEqualTo("Barber Atlas");
        assertThat(list.get(1).position()).isEqualTo(2);
    }
}
