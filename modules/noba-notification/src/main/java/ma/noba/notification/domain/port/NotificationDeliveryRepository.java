package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationDelivery;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationDeliveryRepository {

    NotificationDelivery saveDelivery(NotificationDelivery delivery);

    List<NotificationDelivery> findByNotificationId(UUID notificationId);

    List<NotificationDelivery> findDue(Instant now, Pageable pageable);
}
