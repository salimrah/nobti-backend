package ma.noba.queue.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A client's interest in a provider's queue for when they cannot join right now (closed, paused or
 * full). Unlike a queue membership, no position or estimated wait is reserved — the client is only
 * notified when the queue opens / has room and can then join.
 */
@Entity
@Table(name = "queue_waitlist", indexes = {
        @Index(name = "idx_queue_waitlist_provider", columnList = "provider_id"),
        @Index(name = "idx_queue_waitlist_client", columnList = "client_id")
})
@Getter
@Setter
@NoArgsConstructor
public class QueueWaitlistEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QueueWaitlistStatus status = QueueWaitlistStatus.WAITING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public QueueWaitlistEntry(UUID providerId, UUID clientId) {
        this.providerId = providerId;
        this.clientId = clientId;
        this.status = QueueWaitlistStatus.WAITING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == QueueWaitlistStatus.WAITING || status == QueueWaitlistStatus.NOTIFIED;
    }

    public void markNotified() {
        this.status = QueueWaitlistStatus.NOTIFIED;
        this.notifiedAt = Instant.now();
    }

    public void cancel() {
        this.status = QueueWaitlistStatus.CANCELLED;
    }

    public void convert() {
        this.status = QueueWaitlistStatus.CONVERTED;
    }
}
