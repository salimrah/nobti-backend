package ma.noba.shared.infra.webhook;

import ma.noba.shared.domain.port.WebhookSubscriptionPort;
import ma.noba.shared.domain.webhook.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookSubscriptionRepository
        extends JpaRepository<WebhookSubscription, UUID>, WebhookSubscriptionPort {

    List<WebhookSubscription> findByEventTypeAndIsActiveTrue(String eventType);

    List<WebhookSubscription> findByIsActiveTrue();

    @Override
    default List<WebhookSubscription> findActiveSubscriptions() {
        return findByIsActiveTrue();
    }

    @Override
    default List<WebhookSubscription> findActiveByEventType(String eventType) {
        return findByEventTypeAndIsActiveTrue(eventType);
    }

    @Override
    default Optional<WebhookSubscription> findSubscription(UUID id) {
        return findById(id);
    }

    @Override
    default WebhookSubscription saveSubscription(WebhookSubscription subscription) {
        return save(subscription);
    }

    @Override
    default void deleteSubscription(UUID id) {
        deleteById(id);
    }
}
