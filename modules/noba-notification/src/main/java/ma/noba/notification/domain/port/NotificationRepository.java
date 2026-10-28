package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification saveNotification(Notification notification);

    Optional<Notification> findById(UUID id);

    Optional<Notification> findByDedupeKey(String dedupeKey);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    int markRead(UUID id, UUID userId, Instant readAt);

    int markAllRead(UUID userId, Instant readAt);
}
