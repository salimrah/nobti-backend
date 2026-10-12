package ma.noba.provider.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.provider.application.dto.ProviderSettingsRequest;
import ma.noba.provider.application.dto.ProviderSettingsResponse;
import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Reads and updates per-provider configuration. Missing rows resolve to defaults and are only
 * persisted on the first update, so no row is required at onboarding.
 */
@Service
@RequiredArgsConstructor
public class ProviderSettingsService {

    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "fr", "es", "ar");

    private final ProviderSettingsRepository repository;

    @Transactional(readOnly = true)
    public ProviderSettingsResponse get(UUID providerId) {
        return toResponse(repository.findByProviderId(providerId)
                .orElseGet(() -> ProviderSettings.defaults(providerId)));
    }

    @Transactional
    public ProviderSettingsResponse update(UUID providerId, ProviderSettingsRequest request) {
        ProviderSettings settings = repository.findByProviderId(providerId)
                .orElseGet(() -> ProviderSettings.defaults(providerId));
        settings.apply(
                request.maxPartySize(),
                request.walkInsEnabled(),
                request.appointmentsEnabled(),
                request.noShowGraceMinutes(),
                request.cancellationWindowMinutes(),
                request.autoFillEnabled(),
                request.loyaltyPointsPerVisit(),
                normalizeLocale(request.notificationLocale()));
        return toResponse(repository.save(settings));
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return ProviderSettings.DEFAULT_LOCALE;
        }
        String normalized = locale.trim().toLowerCase();
        return SUPPORTED_LOCALES.contains(normalized) ? normalized : ProviderSettings.DEFAULT_LOCALE;
    }

    private ProviderSettingsResponse toResponse(ProviderSettings s) {
        return new ProviderSettingsResponse(
                s.getProviderId().toString(),
                s.getMaxPartySize(),
                s.isWalkInsEnabled(),
                s.isAppointmentsEnabled(),
                s.getNoShowGraceMinutes(),
                s.getCancellationWindowMinutes(),
                s.isAutoFillEnabled(),
                s.getLoyaltyPointsPerVisit(),
                s.getNotificationLocale());
    }
}
