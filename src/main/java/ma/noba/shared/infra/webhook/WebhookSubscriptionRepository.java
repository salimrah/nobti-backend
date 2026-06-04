package ma.noba.shared.infra.webhook;

import ma.noba.shared.domain.webhook.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByEventTypeAndIsActiveTrue(String eventType);
    List<WebhookSubscription> findByIsActiveTrue();
}
