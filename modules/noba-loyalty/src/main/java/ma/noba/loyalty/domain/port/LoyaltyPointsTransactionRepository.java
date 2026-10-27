package ma.noba.loyalty.domain.port;

import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;

import java.util.List;
import java.util.UUID;

/**
 * Persistence port for loyalty point movements.
 *
 * <p>Application services depend on this interface, never on the Spring Data adapter, so the
 * loyalty module can change its storage without leaking JPA into the use-case layer.
 */
public interface LoyaltyPointsTransactionRepository {

    List<LoyaltyPointsTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LoyaltyPointsTransaction> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    LoyaltyPointsTransaction saveTransaction(LoyaltyPointsTransaction transaction);
}
