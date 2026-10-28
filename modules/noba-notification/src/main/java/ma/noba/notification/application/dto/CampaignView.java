package ma.noba.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CampaignView(
        UUID id,
        String title,
        String body,
        String audience,
        String status,
        int totalRecipients,
        int sentCount,
        int failedCount,
        UUID createdBy,
        Instant createdAt) {
}
