package ma.noba.user.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preference")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationPreference {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public NotificationPreference(UUID userId) {
        this.userId = userId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void setPushEnabled(boolean enabled) {
        this.pushEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void setEmailEnabled(boolean enabled) {
        this.emailEnabled = enabled;
        this.updatedAt = Instant.now();
    }

    public void setSmsEnabled(boolean enabled) {
        this.smsEnabled = enabled;
        this.updatedAt = Instant.now();
    }
}