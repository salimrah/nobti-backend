package ma.noba.provider;

import ma.noba.provider.application.dto.ProviderSettingsRequest;
import ma.noba.provider.application.service.ProviderSettingsService;
import ma.noba.provider.domain.model.ProviderSettings;
import ma.noba.provider.domain.port.ProviderSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderSettingsServiceTest {

    @Mock ProviderSettingsRepository repository;

    private ProviderSettingsService service;
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProviderSettingsService(repository);
    }

    @Test
    @DisplayName("Absent settings resolve to defaults")
    void getReturnsDefaultsWhenAbsent() {
        when(repository.findByProviderId(providerId)).thenReturn(Optional.empty());

        var response = service.get(providerId);

        assertThat(response.providerId()).isEqualTo(providerId.toString());
        assertThat(response.maxPartySize()).isEqualTo(ProviderSettings.DEFAULT_MAX_PARTY_SIZE);
        assertThat(response.walkInsEnabled()).isTrue();
        assertThat(response.appointmentsEnabled()).isTrue();
        assertThat(response.cancellationWindowMinutes()).isEqualTo(ProviderSettings.DEFAULT_CANCELLATION_WINDOW_MINUTES);
        assertThat(response.loyaltyPointsPerVisit()).isEqualTo(ProviderSettings.DEFAULT_LOYALTY_POINTS_PER_VISIT);
        assertThat(response.notificationLocale()).isEqualTo("en");
    }

    @Test
    @DisplayName("Update persists values and normalizes the locale")
    void updatePersistsAndNormalizesLocale() {
        when(repository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(providerId, new ProviderSettingsRequest(
                4, false, true, 5, 30, false, 20, "FR"));

        assertThat(response.maxPartySize()).isEqualTo(4);
        assertThat(response.walkInsEnabled()).isFalse();
        assertThat(response.noShowGraceMinutes()).isEqualTo(5);
        assertThat(response.cancellationWindowMinutes()).isEqualTo(30);
        assertThat(response.autoFillEnabled()).isFalse();
        assertThat(response.loyaltyPointsPerVisit()).isEqualTo(20);
        assertThat(response.notificationLocale()).isEqualTo("fr");
    }

    @Test
    @DisplayName("Unsupported or blank locale falls back to English")
    void unsupportedLocaleFallsBackToDefault() {
        when(repository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.update(providerId, new ProviderSettingsRequest(10, true, true, 10, 60, true, 10, "de"))
                .notificationLocale()).isEqualTo("en");
        assertThat(service.update(providerId, new ProviderSettingsRequest(10, true, true, 10, 60, true, 10, " "))
                .notificationLocale()).isEqualTo("en");
    }
}
