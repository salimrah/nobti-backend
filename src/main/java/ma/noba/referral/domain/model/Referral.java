package ma.noba.referral.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Referral {

    public enum Status {
        PENDING, COMPLETED, EXPIRED
    }

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID referrerId;

    private UUID refereeId;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private boolean rewardClaimed;

    @Column(length = 50)
    private String rewardType;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Referral(UUID referrerId, String code) {
        this.referrerId = referrerId;
        this.code = code;
        this.status = Status.PENDING;
        this.rewardClaimed = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void complete(UUID refereeId, String rewardType) {
        this.refereeId = refereeId;
        this.status = Status.COMPLETED;
        this.rewardType = rewardType;
        this.updatedAt = Instant.now();
    }

    public void claimReward() {
        this.rewardClaimed = true;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.status = Status.EXPIRED;
        this.updatedAt = Instant.now();
    }
}
