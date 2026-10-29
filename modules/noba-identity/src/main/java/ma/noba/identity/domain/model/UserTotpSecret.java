package ma.noba.identity.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.noba.shared.domain.crypto.AesGcmStringAttributeConverter;

import java.time.Instant;
import java.util.UUID;

/** TOTP second factor for an account (secret encrypted at rest). */
@Entity
@Table(name = "user_totp_secret")
@Getter
@Setter
@NoArgsConstructor
public class UserTotpSecret {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 255)
    @Convert(converter = AesGcmStringAttributeConverter.class)
    private String secret;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /** Highest time step already accepted, so a code cannot be replayed. */
    @Column(name = "last_used_step")
    private Long lastUsedStep;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
