package ma.noba.loyalty;

import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import ma.noba.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loyalty is awarded from an {@code AFTER_COMMIT} listener, so the award must run in its own
 * transaction. Without that the repository write throws and Spring swallows the exception inside
 * {@code afterCompletion}, silently losing the user's points.
 */
class LoyaltyAwardIntegrationTest extends AbstractIntegrationTest {

    @Autowired LoyaltyApplicationService loyalty;
    @Autowired LoyaltyPointsRepository points;
    @Autowired PlatformTransactionManager transactions;

    @Test
    @DisplayName("Points awarded from an after-commit listener are persisted")
    void awardFromAfterCommitListener() {
        UUID userId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();

        new TransactionTemplate(transactions).executeWithoutResult(status ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        loyalty.awardPoints(userId, providerId, 10, "SERVICE_COMPLETED", UUID.randomUUID());
                    }
                }));

        assertThat(points.findByUserIdAndProviderId(userId, providerId))
                .hasValueSatisfying(p -> assertThat(p.getTotalPoints()).isEqualTo(10));
    }
}
