package ma.noba.payment.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Every provider webhook received, with the data needed to replay a failed one. */
@Entity
@Table(name = "payment_webhook_event")
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "event_type", length = 80)
    private String eventType;

    @Column(name = "payment_intent_id", length = 100)
    private String paymentIntentId;

    @Column(name = "charge_id", length = 100)
    private String chargeId;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(columnDefinition = "text")
    private String payload;

    /** RECEIVED, PROCESSED, FAILED or REJECTED (signature/parse failure). */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "replay_count", nullable = false)
    private int replayCount;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (receivedAt == null) receivedAt = Instant.now();
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.processedAt = Instant.now();
        this.errorMessage = error == null ? null : error.substring(0, Math.min(500, error.length()));
    }
}
