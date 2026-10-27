package ma.noba.payment.domain.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the authoritative price of a provider's service so checkout amounts are calculated
 * server-side rather than trusted from the client.
 */
public interface ProviderServicePricePort {

    /**
     * @return the active service price for the given provider, or empty when the service does not
     *         exist, is not owned by the provider, is inactive, or has no price configured.
     */
    Optional<BigDecimal> findActiveServicePrice(UUID providerId, UUID serviceId);
}
