package ma.noba.provider.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.booking.domain.port.service.ProviderBookingPolicyPort;
import ma.noba.config.FeatureFlags;
import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderBookingPolicyAdapter implements ProviderBookingPolicyPort {

    private final ProviderSettingsRepository repository;
    private final FeatureFlags featureFlags;

    @Override
    public BookingPolicy policy(UUID providerId) {
        if (!featureFlags.isEnabled("providerSettings", true)) {
            return new BookingPolicy(true, ProviderSettings.DEFAULT_CANCELLATION_WINDOW_MINUTES);
        }
        return repository.findByProviderId(providerId)
                .map(s -> new BookingPolicy(s.isAppointmentsEnabled(), s.getCancellationWindowMinutes()))
                .orElseGet(() -> new BookingPolicy(true, ProviderSettings.DEFAULT_CANCELLATION_WINDOW_MINUTES));
    }
}
