package ma.noba.loyalty.infra;

import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;
import ma.noba.loyalty.domain.port.LoyaltyPointsTransactionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data adapter for {@link LoyaltyPointsTransactionRepository}.
 */
public interface JpaLoyaltyPointsTransactionRepository
        extends JpaRepository<LoyaltyPointsTransaction, java.util.UUID>, LoyaltyPointsTransactionRepository {

    @Override
    default LoyaltyPointsTransaction saveTransaction(LoyaltyPointsTransaction transaction) {
        return save(transaction);
    }
}
