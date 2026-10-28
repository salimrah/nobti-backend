package ma.noba.notification.application.dto;

import ma.noba.notification.domain.model.NotificationTemplate;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String templateKey,
        String channel,
        String locale,
        int version,
        String subject,
        String body,
        boolean active,
        Instant createdAt
) {
    public static TemplateResponse from(NotificationTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getTemplateKey(),
                template.getChannel(),
                template.getLocale(),
                template.getVersion(),
                template.getSubject(),
                template.getBody(),
                template.isActive(),
                template.getCreatedAt());
    }
}
