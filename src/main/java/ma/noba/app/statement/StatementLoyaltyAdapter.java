package ma.noba.app.statement;

import lombok.RequiredArgsConstructor;
import ma.noba.loyalty.domain.port.LoyaltyPointsTransactionRepository;
import ma.noba.statement.domain.port.StatementLoyaltySource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatementLoyaltyAdapter implements StatementLoyaltySource {

    private final LoyaltyPointsTransactionRepository loyaltyTransactions;

    @Override
    public List<LoyaltyLine> findLoyaltyMovements(UUID userId) {
        return loyaltyTransactions.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(tx -> new LoyaltyLine(tx.getCreatedAt(), tx.getPoints(), tx.getReason(), tx.getReferenceId()))
                .toList();
    }
}
