package ma.noba.provider.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_favorite", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_id", "customer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderFavorite {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private Instant createdAt;

    public ProviderFavorite(UUID providerId, UUID customerId) {
        this.providerId = providerId;
        this.customerId = customerId;
        this.createdAt = Instant.now();
    }
}
