package ma.noba.notification;

import ma.noba.notification.application.service.NotificationApplicationService;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock NotificationRepository notifications;
    @Mock NotificationDeliveryRepository deliveries;
    @Mock NotificationEventRepository events;
    @Mock NotificationPreferencePort preferences;

    private NotificationApplicationService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(notifications, deliveries, events, preferences);
        lenient().when(notifications.saveNotification(any())).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });
        lenient().when(deliveries.saveDelivery(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NotificationPort.Command command(Set<NotificationPort.Channel> channels) {
        return new NotificationPort.Command(
                userId, "test.event", NotificationPort.Category.TRANSACTIONAL, 1,
                "Title", "Body", Map.of("k", "v"), "dedupe-key", "en", channels, null, null);
    }

    @Test
    @DisplayName("Creates the inbox item and queues enabled external channels")
    void createsInboxAndQueuesPush() {
        when(preferences.isEnabled(userId, NotificationChannel.PUSH)).thenReturn(true);

        UUID id = service.send(command(Set.of(NotificationPort.Channel.PUSH)));

        assertThat(id).isNotNull();
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveries, times(2)).saveDelivery(deliveryCaptor.capture());
        assertThat(deliveryCaptor.getAllValues())
                .extracting(NotificationDelivery::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.PUSH);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).saveNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.QUEUED);
    }

    @Test
    @DisplayName("A duplicate dedupe key returns the existing notification")
    void dedupeReturnsExisting() {
        Notification existing = Notification.create(userId, "test.event", null, 1, "T", "B", Map.of(), "dedupe-key", "en");
        existing.setId(UUID.randomUUID());
        when(notifications.findByDedupeKey("dedupe-key")).thenReturn(Optional.of(existing));

        UUID id = service.send(command(Set.of(NotificationPort.Channel.PUSH)));

        assertThat(id).isEqualTo(existing.getId());
        verify(notifications, never()).saveNotification(any());
        verify(deliveries, never()).saveDelivery(any());
    }

    @Test
    @DisplayName("A disabled channel is recorded as suppressed, not queued")
    void disabledChannelIsSuppressed() {
        when(preferences.isEnabled(userId, NotificationChannel.PUSH)).thenReturn(false);

        service.send(command(Set.of(NotificationPort.Channel.PUSH)));

        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveries, times(2)).saveDelivery(captor.capture());
        NotificationDelivery push = captor.getAllValues().stream()
                .filter(d -> d.getChannel() == NotificationChannel.PUSH)
                .findFirst().orElseThrow();
        assertThat(push.getStatus().name()).isEqualTo("SUPPRESSED");
        assertThat(push.getErrorCode()).isEqualTo("PREFERENCE_DISABLED");
    }

    @Test
    @DisplayName("markRead returns true only when the user owns an unread notification")
    void markReadOwnership() {
        UUID notificationId = UUID.randomUUID();
        when(notifications.markRead(any(), any(), any())).thenReturn(1);

        assertThat(service.markRead(notificationId, userId)).isTrue();
        verify(events).record(any());
    }

    @Test
    @DisplayName("Unread count is delegated to the repository")
    void unreadCount() {
        when(notifications.countByUserIdAndReadAtIsNull(userId)).thenReturn(3L);
        assertThat(service.unreadCount(userId)).isEqualTo(3);
    }
}
