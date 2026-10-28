package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationDigestEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDigestEntryRepository {

    NotificationDigestEntry saveEntry(NotificationDigestEntry entry);

    /** Users that currently have at least one entry waiting for a digest. */
    List<UUID> findUsersWithPending(int limit);

    List<NotificationDigestEntry> findPendingByUserId(UUID userId);

    /**
     * Entries that existed before the digest window opened. Newer entries belong to the next
     * window, otherwise a user would receive a digest for messages that arrived after it.
     */
    List<NotificationDigestEntry> findPendingBefore(UUID userId, Instant cutoff);

    Optional<NotificationDigestEntry> findByNotificationId(UUID notificationId);

    /** Links every pending entry of the user to the digest summary. */
    int markDigested(UUID userId, UUID digestId);

    /** Links the entries that belonged to the finished window. */
    int markDigestedBefore(UUID userId, UUID digestId, Instant cutoff);

    /** Removes pending entries after they were released as normal deliveries. */
    int deletePendingByUserId(UUID userId);
}
