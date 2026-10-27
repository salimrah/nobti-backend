package ma.noba.subscription.domain.port;

import ma.noba.subscription.domain.model.ProviderSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderSubscriptionRepository {
    ProviderSubscription save(ProviderSubscription subscription);
    Optional<ProviderSubscription> findById(UUID id);
    Optional<ProviderSubscription> findByProviderId(UUID providerId);
    List<ProviderSubscription> findAll();
    List<ProviderSubscription> findByTier(ma.noba.subscription.domain.model.SubscriptionTier tier);
    void delete(ProviderSubscription subscription);
}
