package ma.noba.payment.infra;

import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.model.PayoutStatus;
import ma.noba.payment.domain.port.PayoutRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPayoutRepository extends JpaRepository<Payout, UUID>, PayoutRepository {

    Page<Payout> findByProviderId(UUID providerId, Pageable pageable);

    List<Payout> findByProviderIdAndStatus(UUID providerId, PayoutStatus status);
}
