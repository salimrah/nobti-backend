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

    /**
     * Atomically deducts points only if the balance is sufficient.
     *
     * @return the number of rows updated (1 on success, 0 if the balance was insufficient), so
     *         concurrent redemptions cannot overspend via a read-modify-write.
     */
    int deductPointsIfSufficient(UUID userId, UUID providerId, int points);

    /** Atomically increments the balance, creating the row if it does not exist. */
    void addPoints(UUID userId, UUID providerId, int points);
}
