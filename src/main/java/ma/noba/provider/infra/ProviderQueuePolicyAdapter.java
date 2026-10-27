package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.config.FeatureFlags;
import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import ma.noba.queue.domain.port.service.ProviderQueuePolicyPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderQueuePolicyAdapter implements ProviderQueuePolicyPort {

    private final ProviderSettingsRepository repository;
    private final FeatureFlags featureFlags;

    @Override
    public QueuePolicy policy(UUID providerId) {
        // Platform-level kill switch: disabling `features.providerSettings` restores permissive behaviour.
        if (!featureFlags.isEnabled("providerSettings", true)) {
            return new QueuePolicy(ProviderSettings.DEFAULT_MAX_PARTY_SIZE, true);
        }
        return repository.findByProviderId(providerId)
                .map(s -> new QueuePolicy(s.getMaxPartySize(), s.isWalkInsEnabled()))
                .orElseGet(() -> new QueuePolicy(ProviderSettings.DEFAULT_MAX_PARTY_SIZE, true));
    }
}
