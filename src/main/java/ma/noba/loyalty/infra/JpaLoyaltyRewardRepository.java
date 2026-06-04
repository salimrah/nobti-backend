package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyReward;
import ma.noba.loyalty.domain.port.LoyaltyRewardRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLoyaltyRewardRepository extends JpaRepository<LoyaltyReward, UUID>, LoyaltyRewardRepository {
    List<LoyaltyReward> findByProviderId(UUID providerId);
    List<LoyaltyReward> findByProviderIdAndIsActive(UUID providerId, boolean isActive);
}
