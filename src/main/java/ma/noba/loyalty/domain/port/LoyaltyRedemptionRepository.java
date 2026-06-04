package ma.noba.loyalty.domain.port;

import ma.noba.loyalty.domain.model.LoyaltyRedemption;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyRedemptionRepository {
    LoyaltyRedemption save(LoyaltyRedemption redemption);
    Optional<LoyaltyRedemption> findById(UUID id);
    List<LoyaltyRedemption> findByUserId(UUID userId);
    List<LoyaltyRedemption> findByRewardId(UUID rewardId);
}
