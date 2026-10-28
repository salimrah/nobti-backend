package ma.noba.notification.infra;

import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationDeliveryRepository
        extends JpaRepository<NotificationDelivery, UUID>, NotificationDeliveryRepository {

    @Override
    default NotificationDelivery saveDelivery(NotificationDelivery delivery) {
        return save(delivery);
    }

    List<NotificationDelivery> findByNotificationId(UUID notificationId);

    @Query("select d from NotificationDelivery d " +
            "where d.status in :statuses and d.nextAttemptAt is not null and d.nextAttemptAt <= :now " +
            "order by d.nextAttemptAt asc")
    List<NotificationDelivery> findDueByStatuses(@Param("statuses") Collection<DeliveryStatus> statuses,
                                                 @Param("now") Instant now,
                                                 Pageable pageable);

    @Override
    default List<NotificationDelivery> findDue(Instant now, Pageable pageable) {
        return findDueByStatuses(List.of(DeliveryStatus.QUEUED, DeliveryStatus.FAILED), now, pageable);
    }
}
