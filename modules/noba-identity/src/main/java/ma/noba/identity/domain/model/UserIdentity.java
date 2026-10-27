package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_identities", indexes = {
        @Index(name = "idx_user_identities_user", columnList = "user_id"),
        @Index(name = "idx_user_identities_lookup", columnList = "providerType,identifierHash"),
        @Index(name = "idx_user_identities_primary", columnList = "user_id,isPrimary")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserIdentity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private String providerType;
    private String identifierHash;

    @Convert(converter = ma.noba.shared.domain.crypto.AesGcmStringAttributeConverter.class)
    private String identifierEncrypted;

    @Column(nullable = false)
    private String normalizedIdentifier;
    private boolean isPrimary;
    @Builder.Default private String status = "active";
    private Instant verifiedAt;
    private Instant reverifiedAt;
    @Column(columnDefinition = "TEXT")
    private String scopes;
    private Instant refreshTokenExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
