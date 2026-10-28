package ma.noba.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminDeliveryView(
        UUID id,
        UUID notificationId,
        String notificationType,
        String channel,
        String provider,
        String status,
        int attempt,
        String providerMessageId,
        String errorCode,
        String errorMessage,
        Instant queuedAt,
        Instant sentAt,
        Instant deliveredAt,
        Instant nextAttemptAt,
        Instant createdAt) {
}
