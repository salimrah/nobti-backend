package ma.noba.shared.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_status", columnList = "status"),
        @Index(name = "idx_outbox_created", columnList = "created_at"),
        @Index(name = "idx_outbox_type", columnList = "event_type")
})
public class OutboxEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "retries", nullable = false)
    private int retries;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public OutboxEvent() {
    }

    public static OutboxEvent pending(UUID eventId, String eventType, String aggregateId, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = eventId;
        event.eventType = eventType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = OutboxEventStatus.PENDING;
        event.retries = 0;
        event.createdAt = Instant.now();
        return event;
    }

    public void markSent() {
        this.status = OutboxEventStatus.SENT;
        this.processedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.retries++;
        this.errorMessage = truncate(error, 1000);
        if (this.retries >= 10) {
            this.status = OutboxEventStatus.DEAD_LETTER;
        }
    }

    private String truncate(String str, int maxLen) {
        return str == null ? null : (str.length() <= maxLen ? str : str.substring(0, maxLen));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEvent that = (OutboxEvent) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public OutboxEventStatus getStatus() { return status; }
    public int getRetries() { return retries; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public String getErrorMessage() { return errorMessage; }

    public enum OutboxEventStatus {
        PENDING, SENT, FAILED, DEAD_LETTER
    }
}
