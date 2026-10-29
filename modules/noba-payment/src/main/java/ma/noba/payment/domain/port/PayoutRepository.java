package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.model.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository {
    Payout save(Payout payout);
    Optional<Payout> findById(UUID id);

    Optional<Payout> findByStripeTransferId(String stripeTransferId);
    Page<Payout> findByProviderId(UUID providerId, Pageable pageable);

    Page<Payout> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Payout> findByStatusOrderByCreatedAtDesc(ma.noba.payment.domain.model.PayoutStatus status,
                                                  Pageable pageable);
    List<Payout> findByProviderIdAndStatus(UUID providerId, PayoutStatus status);
}
