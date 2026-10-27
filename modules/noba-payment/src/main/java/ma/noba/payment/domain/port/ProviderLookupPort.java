package ma.noba.payment.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ProviderLookupPort {
    Optional<UUID> findProviderIdByUserId(UUID userId);

    /** Display name of a provider, for read models that would otherwise show a raw id. */
    Optional<String> findProviderName(UUID providerId);
}
