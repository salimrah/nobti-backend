package ma.noba.notification;

import ma.noba.notification.application.dto.ChannelPreferenceDto;
import ma.noba.notification.application.dto.UpdateNotificationPreferencesRequest;
import ma.noba.notification.application.service.NotificationPreferenceService;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationChannelPreference;
import ma.noba.notification.domain.model.NotificationSetting;
import ma.noba.notification.domain.port.NotificationChannelPreferenceRepository;
import ma.noba.notification.domain.port.NotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock NotificationChannelPreferenceRepository channelPreferences;
    @Mock NotificationSettingRepository settings;

    private NotificationPreferenceService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceService(channelPreferences, settings);
    }

    @Test
    @DisplayName("Defaults return every category x channel as enabled with no quiet hours")
    void defaults() {
        when(channelPreferences.findByUserId(userId)).thenReturn(List.of());
        when(settings.findByUserId(userId)).thenReturn(Optional.empty());

        var response = service.get(userId);

        assertThat(response.channels()).hasSize(12);
        assertThat(response.channels()).allMatch(ChannelPreferenceDto::enabled);
        assertThat(response.digest()).isEqualTo("NONE");
        assertThat(response.quietHoursStart()).isNull();
    }

    @Test
    @DisplayName("Update persists the matrix row and the quiet-hours/digest setting")
    void updatePersists() {
        NotificationChannelPreference saved = NotificationChannelPreference.of(
                userId, NotificationCategory.MARKETING, NotificationChannel.PUSH, false);
        when(channelPreferences.find(userId, NotificationCategory.MARKETING, NotificationChannel.PUSH))
                .thenReturn(Optional.empty());
        when(channelPreferences.savePreference(any())).thenReturn(saved);
        when(channelPreferences.findByUserId(userId)).thenReturn(List.of(saved));
        when(settings.findByUserId(userId)).thenReturn(Optional.empty());
        when(settings.saveSetting(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new UpdateNotificationPreferencesRequest(
                List.of(new ChannelPreferenceDto("MARKETING", "PUSH", false)),
                "22:00", "07:00", "Africa/Casablanca", "DAILY");

        var response = service.update(userId, request);

        assertThat(response.channels()).anyMatch(c ->
                c.category().equals("MARKETING") && c.channel().equals("PUSH") && !c.enabled());
        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        verify(settings).saveSetting(captor.capture());
        assertThat(captor.getValue().getQuietHoursStart()).isEqualTo(LocalTime.of(22, 0));
        assertThat(captor.getValue().getQuietHoursEnd()).isEqualTo(LocalTime.of(7, 0));
        assertThat(captor.getValue().getTimezone()).isEqualTo("Africa/Casablanca");
        assertThat(captor.getValue().getDigest()).isEqualTo("DAILY");
    }
}
