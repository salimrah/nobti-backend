package ma.noba.packagegift.domain.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Gift-card module's view of a payment. Keeps gift-card issuance free of the payment module's
 * entities, so checkout/payment can be replaced without touching the gift-card domain.
 */
public interface GiftCardPaymentPort {

    Optional<VerifiedPayment> findVerifiedPayment(UUID paymentTransactionId);

    record VerifiedPayment(UUID id, UUID userId, UUID providerId, BigDecimal amount, boolean succeeded) {
    }
}
