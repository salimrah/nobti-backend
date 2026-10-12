package ma.noba.packagegift.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gift_card")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GiftCard {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "redeemer_id")
    private UUID redeemerId;

    /** Payment transaction that funds this gift card (null for legacy pre-payment cards). */
    @Column(name = "payment_transaction_id")
    private UUID paymentTransactionId;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean redeemed;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public GiftCard(UUID providerId, UUID buyerId, BigDecimal amount, String code) {
        this.providerId = providerId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.code = code;
        this.redeemed = false;
        this.createdAt = Instant.now();
    }

    public void redeem(UUID redeemerId) {
        if (this.redeemed) {
            throw new IllegalStateException("Gift card already redeemed");
        }
        this.redeemerId = redeemerId;
        this.redeemed = true;
        this.redeemedAt = Instant.now();
    }

    public void setRecipientInfo(String recipientName, String message) {
        this.recipientName = recipientName;
        this.message = message;
    }
}
