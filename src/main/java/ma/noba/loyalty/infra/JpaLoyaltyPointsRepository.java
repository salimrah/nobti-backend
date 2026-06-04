package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyPoints;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaLoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, UUID>, LoyaltyPointsRepository {
    Optional<LoyaltyPoints> findByUserIdAndProviderId(UUID userId, UUID providerId);
    List<LoyaltyPoints> findByUserId(UUID userId);
    List<LoyaltyPoints> findByProviderId(UUID providerId);
}