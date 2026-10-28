package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;

import java.time.Instant;
import java.util.List;

/** Read-only aggregates over the notification tables for the admin analytics view/export. */
public interface NotificationAnalyticsRepository {

    long countTotal(Instant from, Instant to);

    long countUnread(Instant from, Instant to);

    List<CategoryTotal> countByCategory(Instant from, Instant to);

    List<TypeTotal> countByType(Instant from, Instant to);

    List<ChannelStatusTotal> countByChannelAndStatus(Instant from, Instant to);

    record CategoryTotal(NotificationCategory category, long count) {
    }

    record TypeTotal(String type, long count) {
    }

    record ChannelStatusTotal(NotificationChannel channel, DeliveryStatus status, long count) {
    }
}
