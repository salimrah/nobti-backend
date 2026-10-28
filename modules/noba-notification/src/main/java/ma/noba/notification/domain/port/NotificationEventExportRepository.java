package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reads the append-only event log for the pseudonymized warehouse export. */
public interface NotificationEventExportRepository {

    /**
     * Keyset page ordered by event id; {@code afterId} is the last id of the previous page.
     */
    List<EventRow> findEvents(Instant from, Instant to, long afterId, int limit);

    record EventRow(
            long id,
            Instant occurredAt,
            String eventType,
            String notificationType,
            NotificationCategory category,
            NotificationChannel channel,
            UUID userId) {
    }
}
