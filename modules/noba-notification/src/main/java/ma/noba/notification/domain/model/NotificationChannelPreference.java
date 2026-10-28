package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Per category x channel opt-in/opt-out for a user. */
@Entity
@Table(name = "notification_channel_preference")
@Getter
@NoArgsConstructor
public class NotificationChannelPreference {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static NotificationChannelPreference of(UUID userId, NotificationCategory category,
                                                   NotificationChannel channel, boolean enabled) {
        NotificationChannelPreference preference = new NotificationChannelPreference();
        preference.userId = userId;
        preference.category = category;
        preference.channel = channel;
        preference.enabled = enabled;
        return preference;
    }

    public void enable(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }
}
