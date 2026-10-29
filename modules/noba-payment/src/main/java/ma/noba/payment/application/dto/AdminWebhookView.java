package ma.noba.payment.application.dto;

import ma.noba.payment.domain.model.PaymentWebhookEvent;

import java.time.Instant;
import java.util.UUID;

public record AdminWebhookView(
        UUID id,
        String provider,
        String eventId,
        String eventType,
        String paymentIntentId,
        String chargeId,
        String failureMessage,
        String status,
        String errorMessage,
        int replayCount,
        Instant receivedAt,
        Instant processedAt,
        String payload) {

    public static AdminWebhookView of(PaymentWebhookEvent event, boolean includePayload) {
        return new AdminWebhookView(
                event.getId(),
                event.getProvider(),
                event.getEventId(),
                event.getEventType(),
                event.getPaymentIntentId(),
                event.getChargeId(),
                event.getFailureMessage(),
                event.getStatus(),
                event.getErrorMessage(),
                event.getReplayCount(),
                event.getReceivedAt(),
                event.getProcessedAt(),
                includePayload ? event.getPayload() : null);
    }
}
