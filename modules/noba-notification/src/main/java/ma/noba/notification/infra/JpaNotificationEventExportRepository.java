package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.port.NotificationEventExportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationEventExportRepository
        extends JpaRepository<NotificationEvent, Long>, NotificationEventExportRepository {

    @Query("select e.id, e.occurredAt, e.eventType, n.type, n.category, d.channel, n.userId " +
            "from NotificationEvent e " +
            "join Notification n on n.id = e.notificationId " +
            "left join NotificationDelivery d on d.id = e.deliveryId " +
            "where e.occurredAt >= :from and e.occurredAt < :to and e.id > :after " +
            "order by e.id asc")
    List<Object[]> pageRows(@Param("from") Instant from, @Param("to") Instant to,
                            @Param("after") long afterId, PageRequest limit);

    @Override
    default List<EventRow> findEvents(Instant from, Instant to, long afterId, int limit) {
        return pageRows(from, to, afterId, PageRequest.of(0, limit)).stream()
                .map(row -> new EventRow(
                        ((Number) row[0]).longValue(),
                        (Instant) row[1],
                        ((NotificationEventType) row[2]).name(),
                        (String) row[3],
                        (NotificationCategory) row[4],
                        (NotificationChannel) row[5],
                        (UUID) row[6]))
                .toList();
    }
}
