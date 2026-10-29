package ma.noba.notification.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Campaign counters plus delivery breakdown by channel and status. */
public record CampaignStatsView(
        UUID campaignId,
        String status,
        int totalRecipients,
        int sentCount,
        int failedCount,
        Instant createdAt,
        Instant scheduledAt,
        Map<String, Map<String, Long>> byChannel) {
}
