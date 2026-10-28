package ma.noba.notification.infra;

import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.port.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepository {

    @Override
    default Notification saveNotification(Notification notification) {
        return save(notification);
    }

    Optional<Notification> findByDedupeKey(String dedupeKey);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt, n.updatedAt = :readAt " +
            "where n.id = :id and n.userId = :userId and n.readAt is null")
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("readAt") Instant readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt, n.updatedAt = :readAt " +
            "where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
