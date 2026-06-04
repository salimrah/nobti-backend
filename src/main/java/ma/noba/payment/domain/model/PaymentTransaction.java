package ma.noba.payment.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentTransaction {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID providerId;

    private UUID serviceId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;

    @Column(name = "stripe_charge_id", length = 100)
    private String stripeChargeId;

    @Column(length = 500)
    private String failureMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    public PaymentTransaction(UUID userId, UUID providerId, UUID serviceId, String currency, BigDecimal amount) {
        this.userId = userId;
        this.providerId = providerId;
        this.serviceId = serviceId;
        this.currency = currency;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markSucceeded(String stripePaymentIntentId, String stripeChargeId, BigDecimal fee) {
        this.status = PaymentStatus.SUCCEEDED;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.stripeChargeId = stripeChargeId;
        this.fee = fee;
        this.netAmount = amount.subtract(fee);
        this.completedAt = Instant.now();
    }

    public void markFailed(String failureMessage) {
        this.status = PaymentStatus.FAILED;
        this.failureMessage = failureMessage;
        this.completedAt = Instant.now();
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
        this.completedAt = Instant.now();
    }
}
