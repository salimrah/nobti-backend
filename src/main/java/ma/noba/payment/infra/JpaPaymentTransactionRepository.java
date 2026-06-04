package ma.noba.payment.infra;

import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, PaymentTransactionRepository {

    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    Page<PaymentTransaction> findByProviderId(UUID providerId, Pageable pageable);

    List<PaymentTransaction> findByProviderIdAndStatus(UUID providerId, PaymentStatus status);
}
