package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyPoints;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaLoyaltyPointsRepository extends JpaRepository<LoyaltyPoints, UUID>, LoyaltyPointsRepository {
    Optional<LoyaltyPoints> findByUserIdAndProviderId(UUID userId, UUID providerId);
    List<LoyaltyPoints> findByUserId(UUID userId);
    List<LoyaltyPoints> findByProviderId(UUID providerId);

    @Override
    @Modifying(flushAutomatically = true)
    @Query("UPDATE LoyaltyPoints lp SET lp.totalPoints = lp.totalPoints - :points, lp.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE lp.userId = :uid AND lp.providerId = :pid AND lp.totalPoints >= :points")
    int deductPointsIfSufficient(@Param("uid") UUID userId, @Param("pid") UUID providerId, @Param("points") int points);

    @Override
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO loyalty_points (id, user_id, provider_id, total_points, created_at, updated_at) " +
                   "VALUES (gen_random_uuid(), :uid, :pid, :points, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT (user_id, provider_id) DO UPDATE " +
                   "SET total_points = loyalty_points.total_points + EXCLUDED.total_points, updated_at = CURRENT_TIMESTAMP",
           nativeQuery = true)
    void addPoints(@Param("uid") UUID userId, @Param("pid") UUID providerId, @Param("points") int points);
}
