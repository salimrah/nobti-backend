package ma.noba.payment.infra;

import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>, PaymentTransactionRepository {

    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.stripePaymentIntentId = :pi")
    Optional<PaymentTransaction> findByStripePaymentIntentIdForUpdate(@Param("pi") String stripePaymentIntentId);

    Page<PaymentTransaction> findByUserId(UUID userId, Pageable pageable);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<PaymentTransaction> findByProviderId(UUID providerId, Pageable pageable);

    List<PaymentTransaction> findByProviderIdAndStatus(UUID providerId, PaymentStatus status);
}
