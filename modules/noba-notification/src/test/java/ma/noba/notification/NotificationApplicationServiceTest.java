package ma.noba.notification;

import ma.noba.notification.application.service.NotificationApplicationService;
import ma.noba.notification.application.service.NotificationTemplateRenderer;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationDigestEntryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.notification.domain.port.NotificationPreferencePort.QuietHours;
import ma.noba.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
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
    @Mock NotificationDigestEntryRepository digestEntries;
    @Mock NotificationTemplateRenderer renderer;

    private NotificationApplicationService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(
                notifications, deliveries, events, preferences, renderer, digestEntries);
        lenient().when(renderer.render(any(), any(), any(), any(), any())).thenAnswer(invocation ->
                new NotificationTemplateRenderer.Rendered(
                        invocation.getArgument(3), invocation.getArgument(4), null, null));
        lenient().when(notifications.saveNotification(any())).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });
        lenient().when(deliveries.saveDelivery(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NotificationPort.Command command(Set<NotificationPort.Channel> channels) {
        return command(NotificationPort.Category.TRANSACTIONAL, channels);
    }

    private NotificationPort.Command command(NotificationPort.Category category,
                                             Set<NotificationPort.Channel> channels) {
        return new NotificationPort.Command(
                userId, "test.event", category, 1,
                "Title", "Body", Map.of("k", "v"), "dedupe-key", "en", channels, null, null);
    }

    @Test
    @DisplayName("Creates the inbox item and queues enabled external channels")
    void createsInboxAndQueuesPush() {
        when(preferences.isEnabled(userId, NotificationCategory.TRANSACTIONAL, NotificationChannel.PUSH)).thenReturn(true);

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
    @DisplayName("Marketing notifications are deferred until quiet hours end")
    void marketingIsDeferredDuringQuietHours() {
        LocalTime now = LocalTime.now(ZoneId.of("UTC"));
        when(preferences.isEnabled(userId, NotificationCategory.MARKETING, NotificationChannel.PUSH)).thenReturn(true);
        when(preferences.quietHours(userId)).thenReturn(Optional.of(
                new QuietHours(now.minusHours(1), now.plusHours(1), "UTC")));

        service.send(command(NotificationPort.Category.MARKETING, Set.of(NotificationPort.Channel.PUSH)));

        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(deliveries, times(2)).saveDelivery(captor.capture());
        NotificationDelivery push = captor.getAllValues().stream()
                .filter(d -> d.getChannel() == NotificationChannel.PUSH)
                .findFirst().orElseThrow();
        assertThat(push.getNextAttemptAt()).isAfter(Instant.now());
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
        when(preferences.isEnabled(userId, NotificationCategory.TRANSACTIONAL, NotificationChannel.PUSH)).thenReturn(false);

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
    @DisplayName("Marketing notifications are held for the digest instead of being pushed")
    void marketingHeldForDigest() {
        when(preferences.digest(userId)).thenReturn(Optional.of(
                new NotificationPreferencePort.Digest(
                        ma.noba.notification.domain.model.DigestFrequency.DAILY, "Africa/Casablanca")));

        service.send(command(NotificationPort.Category.MARKETING, Set.of(NotificationPort.Channel.PUSH)));

        // Only the in-app delivery is created; the push waits for the digest.
        verify(deliveries, times(1)).saveDelivery(any());
        verify(digestEntries).saveEntry(any());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).saveNotification(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.QUEUED);
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
