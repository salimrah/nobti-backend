package ma.noba.app.provider;

import lombok.RequiredArgsConstructor;
import ma.noba.provider.domain.port.ProviderSubscriptionPort;
import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionTier;
import ma.noba.subscription.domain.port.ProviderSubscriptionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Composition-root adapter implementing the provider module's subscription contract with the
 * subscription module's entities. Neither module sees the other's internals.
 */
@Component
@RequiredArgsConstructor
public class ProviderSubscriptionAdapter implements ProviderSubscriptionPort {

    private final ProviderSubscriptionRepository subscriptions;

    @Override
    public void createFree(UUID providerId) {
        subscriptions.save(new ProviderSubscription(providerId, SubscriptionTier.FREE));
    }

    @Override
    public int maxServices(UUID providerId) {
        return subscriptions.findByProviderId(providerId)
                .map(ProviderSubscription::getMaxServices)
                .orElse(1);
    }

    @Override
    public String tier(UUID providerId) {
        return subscriptions.findByProviderId(providerId)
                .map(ProviderSubscription::getTier)
                .map(Enum::name)
                .orElse(SubscriptionTier.FREE.name());
    }
}
