package ma.noba.loyalty.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loyalty_redemption")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LoyaltyRedemption {

    public enum Status {
        PENDING, CONFIRMED, REJECTED
    }

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID rewardId;

    @Column(nullable = false)
    private int pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public LoyaltyRedemption(UUID userId, UUID rewardId, int pointsSpent) {
        this.userId = userId;
        this.rewardId = rewardId;
        this.pointsSpent = pointsSpent;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        this.status = Status.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.updatedAt = Instant.now();
    }
}
