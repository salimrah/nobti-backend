package ma.noba.payment.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provider_payout_settings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProviderPayoutSettings {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID providerId;

    @Column(name = "stripe_account_id", length = 100)
    private String stripeAccountId;

    @Column(name = "payouts_enabled")
    private boolean payoutsEnabled;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public ProviderPayoutSettings(UUID providerId) {
        this.providerId = providerId;
        this.payoutsEnabled = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Sets the connected account and whether payouts are enabled. {@code payoutsEnabled} must come
     * from a verified Stripe account status, never merely from a non-null account id.
     */
    public void updateStripeAccount(String stripeAccountId, boolean payoutsEnabled) {
        this.stripeAccountId = stripeAccountId;
        this.payoutsEnabled = stripeAccountId != null && payoutsEnabled;
        this.updatedAt = Instant.now();
    }
}
