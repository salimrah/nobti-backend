package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.NotificationAnalyticsResponse;
import ma.noba.notification.domain.port.NotificationAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class NotificationAnalyticsService {

    private static final Duration DEFAULT_WINDOW = Duration.ofDays(30);
    private static final Duration MAX_WINDOW = Duration.ofDays(366);

    private final NotificationAnalyticsRepository analytics;

    @Transactional(readOnly = true)
    public NotificationAnalyticsResponse summary(Instant from, Instant to) {
        Instant[] range = normalize(from, to);
        return new NotificationAnalyticsResponse(
                range[0],
                range[1],
                analytics.countTotal(range[0], range[1]),
                analytics.countUnread(range[0], range[1]),
                analytics.countByCategory(range[0], range[1]).stream()
                        .map(c -> new NotificationAnalyticsResponse.CategoryCount(c.category().name(), c.count()))
                        .toList(),
                analytics.countByType(range[0], range[1]).stream()
                        .map(t -> new NotificationAnalyticsResponse.TypeCount(t.type(), t.count()))
                        .toList(),
                analytics.countByChannelAndStatus(range[0], range[1]).stream()
                        .map(c -> new NotificationAnalyticsResponse.ChannelStatusCount(
                                c.channel().name(), c.status().name(), c.count()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public String csv(Instant from, Instant to) {
        NotificationAnalyticsResponse data = summary(from, to);
        StringBuilder csv = new StringBuilder("metric,key,status,count\n");
        csv.append("summary,total,,").append(data.total()).append('\n');
        csv.append("summary,unread,,").append(data.unread()).append('\n');
        data.byCategory().forEach(c ->
                csv.append("category,").append(c.category()).append(",,").append(c.count()).append('\n'));
        data.byType().forEach(t ->
                csv.append("type,").append(escape(t.type())).append(",,").append(t.count()).append('\n'));
        data.byChannel().forEach(c ->
                csv.append("channel,").append(c.channel()).append(',').append(c.status())
                        .append(',').append(c.count()).append('\n'));
        return csv.toString();
    }

    private static Instant[] normalize(Instant from, Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(DEFAULT_WINDOW) : from;
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        if (Duration.between(start, end).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException("Range must not exceed 366 days");
        }
        return new Instant[]{start, end};
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
}
