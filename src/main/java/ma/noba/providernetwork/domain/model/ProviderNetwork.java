package ma.noba.providernetwork.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_network",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "partner_id"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderNetwork {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "partner_name", length = 200)
    private String partnerName;

    @Column(name = "relationship_type", length = 50)
    private String relationshipType;

    @Column(nullable = false)
    private Instant createdAt;

    public ProviderNetwork(UUID providerId, UUID partnerId, String partnerName, String relationshipType) {
        this.providerId = providerId;
        this.partnerId = partnerId;
        this.partnerName = partnerName;
        this.relationshipType = relationshipType;
        this.createdAt = Instant.now();
    }
}
