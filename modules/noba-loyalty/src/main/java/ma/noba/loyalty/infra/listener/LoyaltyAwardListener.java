package ma.noba.loyalty.infra.listener;

import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.loyalty.domain.port.service.ProviderLoyaltyPolicyPort;
import ma.noba.queue.domain.event.ClientServiceCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Awards loyalty points when a queue service is completed. The per-provider rate comes from
 * {@link ProviderLoyaltyPolicyPort}; a rate of 0 disables awarding for that provider.
 */
@Component
public class LoyaltyAwardListener {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyAwardListener.class);

    private final LoyaltyApplicationService loyaltyService;
    private final ProviderLoyaltyPolicyPort loyaltyPolicy;

    public LoyaltyAwardListener(LoyaltyApplicationService loyaltyService,
                                ProviderLoyaltyPolicyPort loyaltyPolicy) {
        this.loyaltyService = loyaltyService;
        this.loyaltyPolicy = loyaltyPolicy;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServiceCompleted(ClientServiceCompletedEvent event) {
        int points = loyaltyPolicy.pointsPerVisit(event.providerId());
        if (points <= 0) {
            log.info("Loyalty awarding disabled for provider {}; skipping", event.providerId());
            return;
        }
        log.info("Awarding {} loyalty points to client {} at provider {}",
                points, event.clientId(), event.providerId());
        loyaltyService.awardPoints(event.clientId(), event.providerId(), points,
                "SERVICE_COMPLETED", event.queueId());
    }
}
