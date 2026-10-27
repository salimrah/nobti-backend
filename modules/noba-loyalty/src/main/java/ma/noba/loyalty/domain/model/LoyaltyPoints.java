package ma.noba.loyalty.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_points", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "providerId"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoyaltyPoints {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private int totalPoints;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public LoyaltyPoints(UUID userId, UUID providerId) {
        this.userId = userId;
        this.providerId = providerId;
        this.totalPoints = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addPoints(int points) {
        this.totalPoints += points;
        this.updatedAt = Instant.now();
    }

    public void deductPoints(int points) {
        if (this.totalPoints < points) {
            throw new IllegalStateException("Insufficient loyalty points");
        }
        this.totalPoints -= points;
        this.updatedAt = Instant.now();
    }

    public String getTier() {
        if (totalPoints >= 1000) return "GOLD";
        if (totalPoints >= 500) return "SILVER";
        return "BRONZE";
    }
}
