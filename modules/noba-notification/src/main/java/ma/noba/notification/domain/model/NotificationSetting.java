package ma.noba.notification.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/** Per-user quiet hours and digest preference. */
@Entity
@Table(name = "notification_setting")
@Getter
@NoArgsConstructor
public class NotificationSetting {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(length = 64)
    private String timezone;

    @Column(nullable = false, length = 16)
    private String digest = "NONE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
        if (digest == null) digest = "NONE";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static NotificationSetting defaults(UUID userId) {
        NotificationSetting setting = new NotificationSetting();
        setting.userId = userId;
        setting.digest = "NONE";
        return setting;
    }

    public void update(LocalTime start, LocalTime end, String timezone, String digest) {
        this.quietHoursStart = start;
        this.quietHoursEnd = end;
        this.timezone = timezone;
        this.digest = digest == null || digest.isBlank() ? "NONE" : digest;
        this.updatedAt = Instant.now();
    }
}
