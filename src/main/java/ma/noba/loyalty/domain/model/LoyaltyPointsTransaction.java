package ma.noba.loyalty.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_points_transaction")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoyaltyPointsTransaction {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 100)
    private String reason;

    private UUID referenceId;

    @Column(nullable = false)
    private Instant createdAt;

    public LoyaltyPointsTransaction(UUID userId, UUID providerId, int points, String reason, UUID referenceId) {
        this.userId = userId;
        this.providerId = providerId;
        this.points = points;
        this.reason = reason;
        this.referenceId = referenceId;
        this.createdAt = Instant.now();
    }
}
