package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One row per channel attempt for a notification. */
@Entity
@Table(name = "notification_delivery")
@Getter
@Setter
@NoArgsConstructor
public class NotificationDelivery {

    @Id
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(length = 32)
    private String provider;

    @Column(name = "recipient_masked", length = 120)
    private String recipientMasked;

    @Column(name = "recipient_hash", length = 64)
    private String recipientHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeliveryStatus status;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "provider_message_id", length = 160)
    private String providerMessageId;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "clicked_at")
    private Instant clickedAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = DeliveryStatus.QUEUED;
    }

    public static NotificationDelivery inApp(UUID notificationId) {
        NotificationDelivery d = new NotificationDelivery();
        d.notificationId = notificationId;
        d.channel = NotificationChannel.IN_APP;
        d.provider = "in-app";
        d.status = DeliveryStatus.SENT;
        d.attempt = 1;
        d.queuedAt = Instant.now();
        d.sentAt = Instant.now();
        return d;
    }

    public static NotificationDelivery queued(UUID notificationId, NotificationChannel channel,
                                              String provider, String recipientMasked) {
        NotificationDelivery d = new NotificationDelivery();
        d.notificationId = notificationId;
        d.channel = channel;
        d.provider = provider;
        d.recipientMasked = recipientMasked;
        d.status = DeliveryStatus.QUEUED;
        d.queuedAt = Instant.now();
        d.nextAttemptAt = Instant.now();
        d.attempt = 0;
        return d;
    }

    public void markSending() {
        this.status = DeliveryStatus.SENDING;
        this.attempt += 1;
    }

    public void markSent(String providerMessageId, Integer latencyMs) {
        this.status = DeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.latencyMs = latencyMs;
        this.sentAt = Instant.now();
        this.nextAttemptAt = null;
        this.errorCode = null;
        this.errorMessage = null;
    }

    /** Schedules a retry; pass {@code null} as nextAttemptAt to make the failure terminal. */
    public void markFailed(String errorCode, String errorMessage, Instant nextAttemptAt) {
        this.status = DeliveryStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage == null ? null : errorMessage.substring(0, Math.min(2000, errorMessage.length()));
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markSuppressed(String errorCode, String errorMessage) {
        this.status = DeliveryStatus.SUPPRESSED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.nextAttemptAt = null;
    }

    public void markDelivered() {
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public boolean terminal() {
        return status == DeliveryStatus.SENT
                || status == DeliveryStatus.DELIVERED
                || status == DeliveryStatus.SUPPRESSED
                || (status == DeliveryStatus.FAILED && nextAttemptAt == null);
    }
}
