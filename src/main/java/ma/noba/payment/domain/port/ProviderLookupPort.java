package ma.noba.payment.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ProviderLookupPort {
    Optional<UUID> findProviderIdByUserId(UUID userId);
}
