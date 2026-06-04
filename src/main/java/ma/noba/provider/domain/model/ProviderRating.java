package ma.noba.provider.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_rating", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_id", "customer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderRating {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private int value;

    private String comment;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderRating(UUID providerId, UUID customerId, int value, String comment) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.providerId = providerId;
        this.customerId = customerId;
        this.value = value;
        this.comment = comment;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(int value, String comment) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        this.value = value;
        this.comment = comment;
        this.updatedAt = Instant.now();
    }
}
