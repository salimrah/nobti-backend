package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLoyaltyPointsTransactionRepository extends JpaRepository<LoyaltyPointsTransaction, UUID> {
    List<LoyaltyPointsTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<LoyaltyPointsTransaction> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
