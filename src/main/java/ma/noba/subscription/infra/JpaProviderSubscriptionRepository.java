package ma.noba.subscription.infra;

import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionTier;
import ma.noba.subscription.domain.port.ProviderSubscriptionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProviderSubscriptionRepository extends JpaRepository<ProviderSubscription, UUID>, ProviderSubscriptionRepository {
    Optional<ProviderSubscription> findByProviderId(UUID providerId);
    List<ProviderSubscription> findByTier(SubscriptionTier tier);
}
