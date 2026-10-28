package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationDigestEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDigestEntryRepository {

    NotificationDigestEntry saveEntry(NotificationDigestEntry entry);

    /** Users that currently have at least one entry waiting for a digest. */
    List<UUID> findUsersWithPending(int limit);

    List<NotificationDigestEntry> findPendingByUserId(UUID userId);

    Optional<NotificationDigestEntry> findByNotificationId(UUID notificationId);

    /** Links every pending entry of the user to the digest summary. */
    int markDigested(UUID userId, UUID digestId);

    /** Removes pending entries after they were released as normal deliveries. */
    int deletePendingByUserId(UUID userId);
}
