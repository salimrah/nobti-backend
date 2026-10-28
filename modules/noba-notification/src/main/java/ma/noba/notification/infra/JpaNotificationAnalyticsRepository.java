package ma.noba.notification.infra;

import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.port.NotificationAnalyticsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationAnalyticsRepository
        extends JpaRepository<Notification, UUID>, NotificationAnalyticsRepository {

    @Query("select count(n) from Notification n where n.createdAt >= :from and n.createdAt < :to")
    long totalInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Override
    default long countTotal(Instant from, Instant to) {
        return totalInRange(from, to);
    }

    @Query("select count(n) from Notification n where n.createdAt >= :from and n.createdAt < :to " +
            "and n.readAt is null")
    long unreadInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Override
    default long countUnread(Instant from, Instant to) {
        return unreadInRange(from, to);
    }

    @Query("select n.category, count(n) from Notification n " +
            "where n.createdAt >= :from and n.createdAt < :to group by n.category")
    List<Object[]> groupByCategory(@Param("from") Instant from, @Param("to") Instant to);

    @Override
    default List<CategoryTotal> countByCategory(Instant from, Instant to) {
        return groupByCategory(from, to).stream()
                .map(row -> new CategoryTotal((NotificationCategory) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Query("select n.type, count(n) from Notification n " +
            "where n.createdAt >= :from and n.createdAt < :to group by n.type order by count(n) desc")
    List<Object[]> groupByType(@Param("from") Instant from, @Param("to") Instant to);

    @Override
    default List<TypeTotal> countByType(Instant from, Instant to) {
        return groupByType(from, to).stream()
                .map(row -> new TypeTotal((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Query("select d.channel, d.status, count(d) from NotificationDelivery d, Notification n " +
            "where d.notificationId = n.id and n.createdAt >= :from and n.createdAt < :to " +
            "group by d.channel, d.status")
    List<Object[]> groupByChannelAndStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Override
    default List<ChannelStatusTotal> countByChannelAndStatus(Instant from, Instant to) {
        return groupByChannelAndStatus(from, to).stream()
                .map(row -> new ChannelStatusTotal((NotificationChannel) row[0],
                        (DeliveryStatus) row[1], ((Number) row[2]).longValue()))
                .toList();
    }
}
