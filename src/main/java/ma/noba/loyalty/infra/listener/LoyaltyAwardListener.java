package ma.noba.loyalty.infra.listener;

import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.queue.domain.event.ClientServiceCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Awards loyalty points when a queue service is completed. Previously {@code awardPoints} had no
 * callers, so balances never grew and rewards could never be redeemed.
 */
@Component
public class LoyaltyAwardListener {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyAwardListener.class);

    private final LoyaltyApplicationService loyaltyService;

    @Value("${loyalty.points-per-service:10}")
    private int pointsPerService;

    public LoyaltyAwardListener(LoyaltyApplicationService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onServiceCompleted(ClientServiceCompletedEvent event) {
        log.info("Awarding {} loyalty points to client {} at provider {}",
                pointsPerService, event.clientId(), event.providerId());
        loyaltyService.awardPoints(event.clientId(), event.providerId(), pointsPerService,
                "SERVICE_COMPLETED", event.queueId());
    }
}
