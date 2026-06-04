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
@Table(name = "payout")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payout {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Column(name = "stripe_transfer_id", length = 100)
    private String stripeTransferId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant paidAt;

    public Payout(UUID providerId, BigDecimal amount, String currency) {
        this.providerId = providerId;
        this.amount = amount;
        this.currency = currency;
        this.status = PayoutStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markProcessing(String stripeTransferId) {
        this.status = PayoutStatus.PROCESSING;
        this.stripeTransferId = stripeTransferId;
    }

    public void markPaid() {
        this.status = PayoutStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void markFailed() {
        this.status = PayoutStatus.FAILED;
    }
}
