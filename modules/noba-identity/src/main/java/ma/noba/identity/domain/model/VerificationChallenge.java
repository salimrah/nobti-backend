package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_challenges", indexes = {
        @Index(name = "idx_verification_user", columnList = "user_id"),
        @Index(name = "idx_verification_active", columnList = "expiresAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VerificationChallenge {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String challengeType;
    private String purpose;
    private String codeHash;
    private int attempts;
    private int maxAttempts;
    private Instant nextResendAt;
    private String deliveryStatus;
    private Instant expiresAt;
    private Instant consumedAt;
    private String createdIp;
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        if (maxAttempts == 0) maxAttempts = 5;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isAttemptsExhausted() {
        return attempts >= maxAttempts;
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void consume() {
        this.consumedAt = Instant.now();
    }
}
