package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository {

    NotificationDelivery saveDelivery(NotificationDelivery delivery);

    List<NotificationDelivery> findByNotificationId(UUID notificationId);

    List<NotificationDelivery> findDue(Instant now, Pageable pageable);

    java.util.Optional<NotificationDelivery> findById(UUID id);

    java.util.Optional<NotificationDelivery> findByProviderMessageId(String providerMessageId);

    Page<DeliveryWithType> searchWithType(DeliveryStatus status, NotificationChannel channel,
                                          Pageable pageable);

    record DeliveryWithType(NotificationDelivery delivery, String notificationType) {
    }

    /** Delivery counters grouped by channel and status for notifications matching a dedupe prefix. */
    List<Object[]> statsForDedupePrefix(String prefix);
}
