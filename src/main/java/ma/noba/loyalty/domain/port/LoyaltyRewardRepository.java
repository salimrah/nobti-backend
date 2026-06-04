package ma.noba.loyalty.domain.port;

import ma.noba.loyalty.domain.model.LoyaltyReward;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyRewardRepository {
    LoyaltyReward save(LoyaltyReward reward);
    Optional<LoyaltyReward> findById(UUID id);
    List<LoyaltyReward> findByProviderId(UUID providerId);
    List<LoyaltyReward> findByProviderIdAndIsActive(UUID providerId, boolean isActive);
    void delete(LoyaltyReward reward);
}
