package ma.noba.notification.application.dto;

import ma.noba.notification.domain.model.Notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String category,
        int priority,
        String title,
        String body,
        Map<String, Object> data,
        String status,
        Instant createdAt,
        Instant readAt,
        boolean unread
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getCategory() != null ? n.getCategory().name() : null,
                n.getPriority(),
                n.getTitle(),
                n.getBody(),
                n.getData(),
                n.getStatus() != null ? n.getStatus().name() : null,
                n.getCreatedAt(),
                n.getReadAt(),
                n.isUnread());
    }
}
