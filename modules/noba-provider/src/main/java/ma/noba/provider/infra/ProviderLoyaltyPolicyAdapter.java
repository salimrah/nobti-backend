package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.config.FeatureFlags;
import ma.noba.loyalty.domain.port.service.ProviderLoyaltyPolicyPort;
import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderLoyaltyPolicyAdapter implements ProviderLoyaltyPolicyPort {

    private final ProviderSettingsRepository repository;
    private final FeatureFlags featureFlags;

    @Override
    public int pointsPerVisit(UUID providerId) {
        if (!featureFlags.isEnabled("providerSettings", true)) {
            return ProviderSettings.DEFAULT_LOYALTY_POINTS_PER_VISIT;
        }
        return repository.findByProviderId(providerId)
                .map(ProviderSettings::getLoyaltyPointsPerVisit)
                .orElse(ProviderSettings.DEFAULT_LOYALTY_POINTS_PER_VISIT);
    }
}
