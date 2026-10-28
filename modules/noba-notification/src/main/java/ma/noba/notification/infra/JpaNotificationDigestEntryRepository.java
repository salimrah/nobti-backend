package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationDigestEntry;
import ma.noba.notification.domain.port.NotificationDigestEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationDigestEntryRepository
        extends JpaRepository<NotificationDigestEntry, UUID>, NotificationDigestEntryRepository {

    @Override
    default NotificationDigestEntry saveEntry(NotificationDigestEntry entry) {
        return save(entry);
    }

    @Query("select e.userId from NotificationDigestEntry e where e.digestId is null group by e.userId")
    List<UUID> findUserIdsWithPending(PageRequest limit);

    @Override
    default List<UUID> findUsersWithPending(int limit) {
        return findUserIdsWithPending(PageRequest.of(0, limit));
    }

    List<NotificationDigestEntry> findByUserIdAndDigestIdIsNullOrderByCreatedAtAsc(UUID userId);

    @Override
    default List<NotificationDigestEntry> findPendingByUserId(UUID userId) {
        return findByUserIdAndDigestIdIsNullOrderByCreatedAtAsc(userId);
    }

    List<NotificationDigestEntry> findByUserIdAndDigestIdIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(
            UUID userId, java.time.Instant cutoff);

    @Override
    default List<NotificationDigestEntry> findPendingBefore(UUID userId, java.time.Instant cutoff) {
        return findByUserIdAndDigestIdIsNullAndCreatedAtBeforeOrderByCreatedAtAsc(userId, cutoff);
    }

    Optional<NotificationDigestEntry> findByNotificationId(UUID notificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationDigestEntry e set e.digestId = :digestId " +
            "where e.userId = :userId and e.digestId is null")
    int linkPending(@Param("userId") UUID userId, @Param("digestId") UUID digestId);

    @Override
    default int markDigested(UUID userId, UUID digestId) {
        return linkPending(userId, digestId);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificationDigestEntry e set e.digestId = :digestId " +
            "where e.userId = :userId and e.digestId is null and e.createdAt < :cutoff")
    int linkPendingBefore(@Param("userId") UUID userId, @Param("digestId") UUID digestId,
                          @Param("cutoff") java.time.Instant cutoff);

    @Override
    default int markDigestedBefore(UUID userId, UUID digestId, java.time.Instant cutoff) {
        return linkPendingBefore(userId, digestId, cutoff);
    }

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificationDigestEntry e where e.userId = :userId and e.digestId is null")
    int deletePending(@Param("userId") UUID userId);

    @Override
    default int deletePendingByUserId(UUID userId) {
        return deletePending(userId);
    }
}
