package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository {
    PaymentTransaction save(PaymentTransaction transaction);
    Optional<PaymentTransaction> findById(UUID id);

    /** Loads the transaction under a pessimistic write lock (used to serialise refunds). */
    Optional<PaymentTransaction> findByIdForUpdate(UUID id);

    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Loads the transaction for the given PaymentIntent while holding a pessimistic write lock, so
     * concurrent/replayed Stripe webhooks are serialised and processed exactly once.
     */
    Optional<PaymentTransaction> findByStripePaymentIntentIdForUpdate(String stripePaymentIntentId);
    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);
    Page<PaymentTransaction> findByProviderId(UUID providerId, Pageable pageable);
    List<PaymentTransaction> findByProviderIdAndStatus(UUID providerId, PaymentStatus status);
}
