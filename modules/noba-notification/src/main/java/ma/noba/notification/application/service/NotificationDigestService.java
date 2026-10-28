package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationDigestEntry;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Bundles marketing notifications for users who asked for a digest. Entries are collected by
 * {@code NotificationApplicationService#send}; this service rolls them into a single summary at the
 * user's local digest hour. If the user turns digests off, pending entries are released as normal
 * push deliveries instead of being dropped.
 */
@Service
@RequiredArgsConstructor
public class NotificationDigestService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDigestService.class);
    private static final int BATCH = 200;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final NotificationDigestEntryRepository entries;
    private final NotificationRepository notifications;
    private final NotificationPreferencePort preferences;
    private final NotificationApplicationService application;

    @Value("${notification.digest.hour:9}")
    private int digestHour = 9;

    /**
     * Flushes every user whose digest is due and releases pending entries for users that no longer
     * want digests.
     *
     * @return number of digests sent or released in this run
     */
    @Transactional
    public int flushDue() {
        List<UUID> userIds = entries.findUsersWithPending(BATCH);
        int processed = 0;
        for (UUID userId : userIds) {
            try {
                if (flush(userId)) {
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
        if (!due(digest, now, digestHour)) {
            return false;
        }
        List<NotificationDigestEntry> pending = entries.findPendingByUserId(userId);
        if (pending.isEmpty()) {
            return false;
        }
        List<Notification> items = new ArrayList<>();
        for (NotificationDigestEntry entry : pending) {
            notifications.findById(entry.getNotificationId()).ifPresent(items::add);
        }
        if (items.isEmpty()) {
            // Everything was deleted in the meantime; clear the queue so we do not spin.
            entries.markDigested(userId, userId);
            return false;
        }

        String day = ZonedDateTime.now(zone(digest.timezone())).format(DAY);
        UUID digestId = application.send(new NotificationPort.Command(
                userId,
                "digest.summary",
                NotificationPort.Category.SYSTEM,
                1,
                "Your updates",
                summary(items),
                Map.of("count", String.valueOf(items.size()),
                        "notifications", items.stream().map(Notification::getId).map(UUID::toString).toList()),
                "digest:" + userId + ":" + day,
                null,
                Set.of(NotificationPort.Channel.PUSH),
                null,
                null));
        entries.markDigested(userId, digestId);
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

    /** True when the user's local clock is inside the digest hour on an allowed day. */
    public static boolean due(NotificationPreferencePort.Digest digest, Instant now, int hour) {
        ZonedDateTime local = now.atZone(zone(digest.timezone()));
        boolean inWindow = local.getHour() == hour;
        if (!inWindow) {
            return false;
        }
        return switch (digest.frequency()) {
            case DAILY -> true;
            case WEEKLY -> local.getDayOfWeek() == DayOfWeek.MONDAY;
            case NONE -> false;
        };
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
