package ma.noba.subscription.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_subscription")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderSubscription {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant renewsAt;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderSubscription(UUID providerId, SubscriptionTier tier) {
        this.providerId = providerId;
        this.tier = tier;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void upgrade(SubscriptionTier newTier) {
        if (newTier.ordinal() <= this.tier.ordinal()) {
            throw new IllegalStateException("New tier must be higher than current tier");
        }
        this.tier = newTier;
        this.updatedAt = Instant.now();
    }

    public void downgrade(SubscriptionTier newTier) {
        if (newTier.ordinal() >= this.tier.ordinal()) {
            throw new IllegalStateException("New tier must be lower than current tier");
        }
        this.tier = newTier;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.tier = SubscriptionTier.FREE;
        this.updatedAt = Instant.now();
    }

    public void setRenewalDate(Instant renewsAt) {
        this.renewsAt = renewsAt;
        this.updatedAt = Instant.now();
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE || this.status == SubscriptionStatus.TRIAL;
    }

    public int getMaxServices() {
        return switch (this.tier) {
            case FREE -> 1;
            case BASIC, PREMIUM, ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public boolean hasAnalyticsAccess() {
        return this.tier.ordinal() >= SubscriptionTier.BASIC.ordinal();
    }

    public boolean hasPromotionsAccess() {
        return this.tier.ordinal() >= SubscriptionTier.PREMIUM.ordinal();
    }

    public boolean hasBadgeAccess() {
        return this.tier.ordinal() >= SubscriptionTier.PREMIUM.ordinal();
    }

    public boolean hasFeaturedPlacement() {
        return this.tier.ordinal() >= SubscriptionTier.PREMIUM.ordinal();
    }

    public boolean hasMultiLocationAccess() {
        return this.tier == SubscriptionTier.ENTERPRISE;
    }

    public boolean hasEmployeeManagementAccess() {
        return this.tier == SubscriptionTier.ENTERPRISE;
    }

    public boolean hasApiAccess() {
        return this.tier == SubscriptionTier.ENTERPRISE;
    }
}
