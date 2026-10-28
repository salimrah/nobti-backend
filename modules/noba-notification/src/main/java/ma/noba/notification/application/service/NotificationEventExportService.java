package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.port.NotificationEventExportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Exports the notification event log for the warehouse. User ids are replaced by a stable HMAC
 * pseudonym, and the response is keyset-paginated so a nightly job can pull the log in chunks
 * without loading it all in memory.
 */
@Service
@RequiredArgsConstructor
public class NotificationEventExportService {

    public static final String HEADER = "event_id,occurred_at,event_type,notification_type,category,channel,pseudo_user\n";
    private static final Duration DEFAULT_WINDOW = Duration.ofDays(7);
    private static final int MAX_LIMIT = 5000;

    private final NotificationEventExportRepository repository;
    private final NotificationPseudonymizer pseudonymizer;

    @Transactional(readOnly = true)
    public ExportPage events(Instant from, Instant to, long afterId, int limit) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(DEFAULT_WINDOW) : from;
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        int pageSize = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<NotificationEventExportRepository.EventRow> rows =
                repository.findEvents(start, end, Math.max(afterId, 0), pageSize);

        StringBuilder csv = new StringBuilder(HEADER);
        for (NotificationEventExportRepository.EventRow row : rows) {
            csv.append(row.id()).append(',')
                    .append(row.occurredAt()).append(',')
                    .append(row.eventType()).append(',')
                    .append(escape(row.notificationType())).append(',')
                    .append(row.category() == null ? "" : row.category().name()).append(',')
                    .append(row.channel() == null ? "" : row.channel().name()).append(',')
                    .append(pseudonymizer.apply(row.userId()))
                    .append('\n');
        }
        long nextAfter = rows.isEmpty() ? Math.max(afterId, 0) : rows.get(rows.size() - 1).id();
        return new ExportPage(csv.toString(), nextAfter, rows.size());
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String safe = value.toLowerCase(Locale.ROOT);
        return safe.contains(",") || safe.contains("\"")
                ? '"' + safe.replace("\"", "\"\"") + '"'
                : safe;
    }

    /** CSV body plus the cursor for the next page ({@code X-Next-After}). */
    public record ExportPage(String csv, long nextAfter, int rows) {
    }
}
