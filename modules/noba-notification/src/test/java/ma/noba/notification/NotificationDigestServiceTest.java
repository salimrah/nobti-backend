package ma.noba.notification;

import ma.noba.notification.application.service.NotificationApplicationService;
import ma.noba.notification.application.service.NotificationDigestService;
import ma.noba.notification.domain.model.DigestFrequency;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationDigestEntry;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationDigestEntryRepository;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDigestServiceTest {

    @Mock NotificationDigestEntryRepository entries;
    @Mock NotificationRepository notifications;
    @Mock NotificationPreferencePort preferences;
    @Mock NotificationApplicationService application;

    private NotificationDigestService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationDigestService(entries, notifications, preferences, application);
    }

    private static Instant utc(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC).toInstant();
    }

    private Notification notification(String title) {
        Notification n = Notification.create(userId, "promo", null, 1, title, "Body", null, null, "en");
        n.setId(UUID.randomUUID());
        return n;
    }

    @Test
    @DisplayName("The daily window is today's digest hour, or yesterday's before it happens")
    void dailyWindow() {
        var digest = new NotificationPreferencePort.Digest(DigestFrequency.DAILY, "UTC");

        assertThat(NotificationDigestService.windowStart(digest, utc(2026, 10, 28, 10), 9))
                .isEqualTo(utc(2026, 10, 28, 9));
        assertThat(NotificationDigestService.windowStart(digest, utc(2026, 10, 28, 8), 9))
                .isEqualTo(utc(2026, 10, 27, 9));
    }

    @Test
    @DisplayName("The weekly window is this Monday's digest hour, or last Monday's before it")
    void weeklyWindow() {
        var digest = new NotificationPreferencePort.Digest(DigestFrequency.WEEKLY, "UTC");

        // Wednesday -> Monday of the same week.
        assertThat(NotificationDigestService.windowStart(digest, utc(2026, 10, 28, 10), 9))
                .isEqualTo(utc(2026, 10, 26, 9));
        // Monday before the hour -> the previous Monday.
        assertThat(NotificationDigestService.windowStart(digest, utc(2026, 11, 2, 8), 9))
                .isEqualTo(utc(2026, 10, 26, 9));
    }

    @Test
    @DisplayName("Turning digests off releases held notifications as pushes")
    void releaseWhenDigestDisabled() {
        NotificationDigestEntry entry = NotificationDigestEntry.of(userId, UUID.randomUUID());
        when(preferences.digest(userId)).thenReturn(Optional.of(
                new NotificationPreferencePort.Digest(DigestFrequency.NONE, "UTC")));
        when(entries.findPendingByUserId(userId)).thenReturn(List.of(entry));

        boolean processed = service.flush(userId, utc(2026, 10, 28, 12));

        assertThat(processed).isTrue();
        verify(application).queuePush(entry.getNotificationId());
        verify(entries).deletePendingByUserId(userId);
    }

    @Test
    @DisplayName("A due digest sends one summary and links the pending entries")
    void sendsSummary() {
        Notification first = notification("Promo A");
        Notification second = notification("Promo B");
        NotificationDigestEntry a = NotificationDigestEntry.of(userId, first.getId());
        NotificationDigestEntry b = NotificationDigestEntry.of(userId, second.getId());
        UUID digestId = UUID.randomUUID();

        when(preferences.digest(userId)).thenReturn(Optional.of(
                new NotificationPreferencePort.Digest(DigestFrequency.DAILY, "UTC")));
        when(entries.findPendingBefore(userId, utc(2026, 10, 28, 9))).thenReturn(List.of(a, b));
        when(notifications.findById(first.getId())).thenReturn(Optional.of(first));
        when(notifications.findById(second.getId())).thenReturn(Optional.of(second));
        when(application.send(any())).thenReturn(digestId);

        boolean processed = service.flush(userId, utc(2026, 10, 28, 10));

        assertThat(processed).isTrue();
        ArgumentCaptor<NotificationPort.Command> captor = ArgumentCaptor.forClass(NotificationPort.Command.class);
        verify(application).send(captor.capture());
        NotificationPort.Command command = captor.getValue();
        assertThat(command.type()).isEqualTo("digest.summary");
        assertThat(command.category()).isEqualTo(NotificationPort.Category.SYSTEM);
        assertThat(command.body()).contains("2 new updates").contains("Promo A");
        assertThat(command.data()).containsEntry("count", "2");
        verify(entries).markDigestedBefore(userId, digestId, utc(2026, 10, 28, 9));
    }

    @Test
    @DisplayName("Entries that arrived after the window wait for the next one")
    void waitsForNextWindow() {
        when(preferences.digest(userId)).thenReturn(Optional.of(
                new NotificationPreferencePort.Digest(DigestFrequency.WEEKLY, "UTC")));
        when(entries.findPendingBefore(userId, utc(2026, 10, 26, 9))).thenReturn(List.of());

        boolean processed = service.flush(userId, utc(2026, 10, 28, 10));

        assertThat(processed).isFalse();
        verify(application, never()).send(any());
        verify(entries, never()).markDigestedBefore(any(), any(), any());
    }

    @Test
    @DisplayName("A missed digest hour is caught up on the next run")
    void catchesUpAfterMissedHour() {
        Notification item = notification("Promo");
        NotificationDigestEntry entry = NotificationDigestEntry.of(userId, item.getId());
        UUID digestId = UUID.randomUUID();
        when(preferences.digest(userId)).thenReturn(Optional.of(
                new NotificationPreferencePort.Digest(DigestFrequency.DAILY, "UTC")));
        when(entries.findPendingBefore(userId, utc(2026, 10, 28, 9))).thenReturn(List.of(entry));
        when(notifications.findById(item.getId())).thenReturn(Optional.of(item));
        when(application.send(any())).thenReturn(digestId);

        // 14:00: the 09:00 window was missed but is still open.
        assertThat(service.flush(userId, utc(2026, 10, 28, 14))).isTrue();
        verify(entries).markDigestedBefore(userId, digestId, utc(2026, 10, 28, 9));
    }
}
