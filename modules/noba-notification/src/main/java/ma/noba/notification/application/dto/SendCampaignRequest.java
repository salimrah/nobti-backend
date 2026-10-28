package ma.noba.notification.application.dto;

public record SendCampaignRequest(
        String title,
        String body,
        String audience) {
}
