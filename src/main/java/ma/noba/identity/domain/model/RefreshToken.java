package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="refresh_tokens", indexes = {
        @Index(name="idx_refresh_tokens_hash", columnList = "tokenHash", unique = true),
        @Index(name="idx_refresh_tokens_user", columnList = "user_id"),
        @Index(name="idx_refresh_tokens_device", columnList = "deviceId"),
        @Index(name="idx_refresh_tokens_family", columnList = "familyId"),
        @Index(name="idx_refresh_tokens_active", columnList = "user_id,expiresAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshToken {
    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id")
    @JsonIgnore
    private User user;

    private UUID deviceId;
    private String tokenHash;
    private UUID familyId;
    private UUID rotationParentId;
    private boolean mfaVerified;
    private String issuedIp;
    private String issuedUserAgent;
    private Instant revokedAt;
    private String revokeReason;
    private Instant reusedAt;
    private String deviceFingerprintHash;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant lastUsedAt;

    @PrePersist void prePersist(){
        if (id==null) id=UUID.randomUUID();
        createdAt=Instant.now();
        lastUsedAt=createdAt;
        if (familyId==null) familyId=UUID.randomUUID();
    }

    public boolean isRevoked() { return revokedAt != null; }

    public void revoke(String reason) {
        this.revokedAt = Instant.now();
        this.revokeReason = reason;
    }
}
