package ma.noba.loyalty.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_reward")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoyaltyReward {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int pointsRequired;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Instant createdAt;

    public LoyaltyReward(UUID providerId, String name, String description, int pointsRequired) {
        this.providerId = providerId;
        this.name = name;
        this.description = description;
        this.pointsRequired = pointsRequired;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
    }
}
