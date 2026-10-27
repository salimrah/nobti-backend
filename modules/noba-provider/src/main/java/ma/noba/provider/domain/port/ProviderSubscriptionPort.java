package ma.noba.provider.domain.port;

import java.util.UUID;

/**
 * Provider-owned view of subscriptions. Keeps the provider module free of the subscription
 * module's entities and of its persistence port, so subscription billing can later be replaced
 * or moved to a service without touching provider code.
 */
public interface ProviderSubscriptionPort {

    /** Creates the default free subscription for a newly created provider. */
    void createFree(UUID providerId);

    /** Maximum number of services allowed by the provider's current plan. */
    int maxServices(UUID providerId);

    /** Tier name (for example {@code FREE}, {@code PREMIUM}); never null. */
    String tier(UUID providerId);
}
