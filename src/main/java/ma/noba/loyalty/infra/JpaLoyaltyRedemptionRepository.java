package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyRedemption;
import ma.noba.loyalty.domain.port.LoyaltyRedemptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLoyaltyRedemptionRepository extends JpaRepository<LoyaltyRedemption, UUID>, LoyaltyRedemptionRepository {
    List<LoyaltyRedemption> findByUserId(UUID userId);
    List<LoyaltyRedemption> findByRewardId(UUID rewardId);
}
