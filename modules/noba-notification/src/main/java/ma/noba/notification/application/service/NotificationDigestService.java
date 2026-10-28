package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationDigestEntry;
import ma.noba.notification.domain.port.NotificationDigestEntryRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.notification.domain.port.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Bundles marketing notifications for users who asked for a digest. Entries are collected by
 * {@code NotificationApplicationService#send}; this service rolls the ones that existed before the
 * most recent scheduled window into a single summary.
 *
 * <p>The window is <em>catch-up aware</em>: if the app was down at the user's digest hour, the
 * window stays open until the next run instead of skipping a whole day (or week). Entries that
 * arrive after the window opened belong to the next window, so a digest never includes messages
 * the user received minutes after it was sent.
 */
@Service
@RequiredArgsConstructor
public class NotificationDigestService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDigestService.class);
    private static final int BATCH = 200;

    private final NotificationDigestEntryRepository entries;
    private final NotificationRepository notifications;
    private final NotificationPreferencePort preferences;
    private final NotificationApplicationService application;

    @Value("${notification.digest.hour:9}")
    private int digestHour = 9;

    /**
     * Flushes every user whose digest window has closed and releases pending entries for users
     * that no longer want digests.
     *
     * @return number of digests sent or released in this run
     */
    @Transactional
    public int flushDue() {
        return flushDue(Instant.now());
    }

    @Transactional
    public int flushDue(Instant now) {
        List<UUID> userIds = entries.findUsersWithPending(BATCH);
        int processed = 0;
        for (UUID userId : userIds) {
            try {
                if (flush(userId, now)) {
                    processed += 1;
                }
            } catch (Exception e) {
                log.error("Digest flush failed for user {}", userId, e);
            }
        }
        return processed;
    }

    @Transactional
    public boolean flush(UUID userId) {
        return flush(userId, Instant.now());
    }

    @Transactional
    public boolean flush(UUID userId, Instant now) {
        Optional<NotificationPreferencePort.Digest> preference = preferences.digest(userId);
        if (preference.isEmpty() || !preference.get().enabled()) {
            return release(userId);
        }
        NotificationPreferencePort.Digest digest = preference.get();
        Instant windowStart = windowStart(digest, now, digestHour);
        List<NotificationDigestEntry> due = entries.findPendingBefore(userId, windowStart);
        if (due.isEmpty()) {
            // Nothing old enough yet; wait for the next window.
            return false;
        }
        List<Notification> items = new ArrayList<>();
        for (NotificationDigestEntry entry : due) {
            notifications.findById(entry.getNotificationId()).ifPresent(items::add);
        }
        if (items.isEmpty()) {
            // The notifications were deleted in the meantime; clear them so we do not spin.
            entries.markDigestedBefore(userId, userId, windowStart);
            return false;
        }

        UUID digestId = application.send(new NotificationPort.Command(
                userId,
                "digest.summary",
                NotificationPort.Category.SYSTEM,
                1,
                "Your updates",
                summary(items),
                Map.of("count", String.valueOf(items.size()),
                        "notifications", items.stream().map(Notification::getId).map(UUID::toString).toList()),
                "digest:" + userId + ":" + windowStart.getEpochSecond(),
                null,
                Set.of(NotificationPort.Channel.PUSH),
                null,
                null));
        entries.markDigestedBefore(userId, digestId, windowStart);
        return true;
    }

    /** Turns pending entries back into normal push deliveries (digests were disabled). */
    @Transactional
    public boolean release(UUID userId) {
        List<NotificationDigestEntry> pending = entries.findPendingByUserId(userId);
        if (pending.isEmpty()) {
            return false;
        }
        for (NotificationDigestEntry entry : pending) {
            application.queuePush(entry.getNotificationId());
        }
        entries.deletePendingByUserId(userId);
        return true;
    }

    static String summary(List<Notification> items) {
        if (items.size() == 1) {
            return items.get(0).getTitle() + ": " + items.get(0).getBody();
        }
        List<String> titles = items.stream().map(Notification::getTitle).distinct().limit(3).toList();
        return "You have " + items.size() + " new updates: " + String.join(", ", titles)
                + (items.size() > titles.size() ? "…" : "");
    }

    /**
     * The most recent scheduled digest instant at or before {@code now}: today at the digest hour
     * (DAILY) or this week's Monday at the digest hour (WEEKLY), stepping back one period when that
     * moment has not happened yet.
     */
    public static Instant windowStart(NotificationPreferencePort.Digest digest, Instant now, int hour) {
        ZonedDateTime local = now.atZone(zone(digest.timezone()));
        ZonedDateTime candidate = local
                .withHour(hour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        if (digest.frequency() == ma.noba.notification.domain.model.DigestFrequency.WEEKLY) {
            candidate = candidate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            if (candidate.isAfter(local)) {
                candidate = candidate.minusWeeks(1);
            }
        } else if (candidate.isAfter(local)) {
            candidate = candidate.minusDays(1);
        }
        return candidate.toInstant();
    }

    private static ZoneId zone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }
}
