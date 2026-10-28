package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A notification held back because the user asked for a digest. {@code digestId} is null until the
 * entry is either rolled into a digest summary or released individually.
 */
@Entity
@Table(name = "notification_digest_entry")
@Getter
@NoArgsConstructor
public class NotificationDigestEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_id", nullable = false, unique = true)
    private UUID notificationId;

    @Column(name = "digest_id")
    private UUID digestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public static NotificationDigestEntry of(UUID userId, UUID notificationId) {
        NotificationDigestEntry entry = new NotificationDigestEntry();
        entry.userId = userId;
        entry.notificationId = notificationId;
        return entry;
    }

    public void attachTo(UUID digestId) {
        this.digestId = digestId;
    }
}
