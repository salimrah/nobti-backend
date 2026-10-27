package ma.noba.provider.domain.port;

import ma.noba.provider.domain.model.ProviderSettings;

import java.util.Optional;
import java.util.UUID;

public interface ProviderSettingsRepository {

    Optional<ProviderSettings> findByProviderId(UUID providerId);

    ProviderSettings save(ProviderSettings settings);
}
