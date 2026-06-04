package ma.noba.loyalty.domain.port;

import ma.noba.loyalty.domain.model.LoyaltyPoints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoyaltyPointsRepository {
    LoyaltyPoints save(LoyaltyPoints points);
    Optional<LoyaltyPoints> findByUserIdAndProviderId(UUID userId, UUID providerId);
    List<LoyaltyPoints> findByUserId(UUID userId);
    List<LoyaltyPoints> findByProviderId(UUID providerId);
}
