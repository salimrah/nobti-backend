package ma.noba.identity.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "consent_type"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserConsent {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String consentType;

    private boolean granted;

    private String consentVersion;

    private String ipAddress;

    private String userAgent;

    private Instant grantedAt;

    private Instant revokedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (grantedAt == null) grantedAt = Instant.now();
    }
}
