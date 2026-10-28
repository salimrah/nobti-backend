package ma.noba.notification.infra;

import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import org.springframework.data.domain.Page;
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

    java.util.Optional<NotificationDelivery> findByProviderMessageId(String providerMessageId);

    @Query("select d, n.type from NotificationDelivery d join Notification n on n.id = d.notificationId " +
            "where (:status is null or d.status = :status) " +
            "and (:channel is null or d.channel = :channel) " +
            "order by d.createdAt desc")
    Page<Object[]> searchRows(@Param("status") DeliveryStatus status,
                              @Param("channel") NotificationChannel channel,
                              Pageable pageable);

    @Override
    default Page<DeliveryWithType> searchWithType(DeliveryStatus status, NotificationChannel channel,
                                                  Pageable pageable) {
        return searchRows(status, channel, pageable)
                .map(row -> new DeliveryWithType((NotificationDelivery) row[0], (String) row[1]));
    }

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
