package ma.noba.provider.infra;

import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProviderSettingsRepository
        extends JpaRepository<ProviderSettings, UUID>, ProviderSettingsRepository {

    @Override
    Optional<ProviderSettings> findByProviderId(UUID providerId);
}
