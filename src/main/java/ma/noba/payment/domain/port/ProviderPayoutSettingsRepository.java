package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.ProviderPayoutSettings;

import java.util.Optional;
import java.util.UUID;

public interface ProviderPayoutSettingsRepository {
    ProviderPayoutSettings save(ProviderPayoutSettings settings);
    Optional<ProviderPayoutSettings> findByProviderId(UUID providerId);
}
