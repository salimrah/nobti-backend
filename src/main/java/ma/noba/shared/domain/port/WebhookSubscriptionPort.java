package ma.noba.shared.domain.port;

import ma.noba.shared.domain.webhook.WebhookSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for webhook subscriptions. The REST controller depends on this port, not
 * on the Spring Data adapter.
 */
public interface WebhookSubscriptionPort {

    List<WebhookSubscription> findActiveSubscriptions();

    List<WebhookSubscription> findActiveByEventType(String eventType);

    Optional<WebhookSubscription> findSubscription(UUID id);

    WebhookSubscription saveSubscription(WebhookSubscription subscription);

    void deleteSubscription(UUID id);
}
