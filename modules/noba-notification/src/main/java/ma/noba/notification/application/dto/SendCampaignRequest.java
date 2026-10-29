package ma.noba.notification.application.dto;

import java.time.Instant;

/** When {@code scheduledAt} is set (and in the future) the campaign waits for the scheduler. */
public record SendCampaignRequest(
        String title,
        String body,
        String audience,
        Instant scheduledAt) {
}
