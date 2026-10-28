package ma.noba.notification.application.dto;

import java.time.Instant;
import java.util.List;

public record NotificationAnalyticsResponse(
        Instant from,
        Instant to,
        long total,
        long unread,
        List<CategoryCount> byCategory,
        List<TypeCount> byType,
        List<ChannelStatusCount> byChannel
) {
    public record CategoryCount(String category, long count) {
    }

    public record TypeCount(String type, long count) {
    }

    public record ChannelStatusCount(String channel, String status, long count) {
    }
}
