package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.ChannelPreferenceDto;
import ma.noba.notification.application.dto.NotificationPreferencesResponse;
import ma.noba.notification.application.dto.UpdateNotificationPreferencesRequest;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationChannelPreference;
import ma.noba.notification.domain.model.NotificationSetting;
import ma.noba.notification.domain.port.NotificationChannelPreferenceRepository;
import ma.noba.notification.domain.port.NotificationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private static final List<NotificationChannel> EXTERNAL_CHANNELS = List.of(
            NotificationChannel.PUSH, NotificationChannel.EMAIL,
            NotificationChannel.SMS, NotificationChannel.WHATSAPP);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationChannelPreferenceRepository channelPreferences;
    private final NotificationSettingRepository settings;

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse get(UUID userId) {
        Map<String, Boolean> existing = new HashMap<>();
        for (NotificationChannelPreference preference : channelPreferences.findByUserId(userId)) {
            existing.put(key(preference.getCategory(), preference.getChannel()), preference.isEnabled());
        }
        List<ChannelPreferenceDto> channels = new ArrayList<>();
        for (NotificationCategory category : NotificationCategory.values()) {
            for (NotificationChannel channel : EXTERNAL_CHANNELS) {
                channels.add(new ChannelPreferenceDto(category.name(), channel.name(),
                        existing.getOrDefault(key(category, channel), true)));
            }
        }
        NotificationSetting setting = settings.findByUserId(userId).orElse(null);
        return new NotificationPreferencesResponse(
                channels,
                setting != null ? format(setting.getQuietHoursStart()) : null,
                setting != null ? format(setting.getQuietHoursEnd()) : null,
                setting != null ? setting.getTimezone() : null,
                setting != null ? setting.getDigest() : "NONE");
    }

    @Transactional
    public NotificationPreferencesResponse update(UUID userId, UpdateNotificationPreferencesRequest request) {
        if (request.channels() != null) {
            for (ChannelPreferenceDto dto : request.channels()) {
                NotificationCategory category = parseCategory(dto.category());
                NotificationChannel channel = parseChannel(dto.channel());
                if (category == null || channel == null || channel == NotificationChannel.IN_APP) {
                    continue;
                }
                NotificationChannelPreference preference = channelPreferences.find(userId, category, channel)
                        .orElseGet(() -> NotificationChannelPreference.of(userId, category, channel, true));
                preference.enable(dto.enabled());
                channelPreferences.savePreference(preference);
            }
        }
        NotificationSetting setting = settings.findByUserId(userId)
                .orElseGet(() -> NotificationSetting.defaults(userId));
        setting.update(parseTime(request.quietHoursStart()), parseTime(request.quietHoursEnd()),
                request.timezone(), request.digest());
        settings.saveSetting(setting);
        return get(userId);
    }

    private static NotificationCategory parseCategory(String value) {
        if (value == null) return null;
        try {
            return NotificationCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static NotificationChannel parseChannel(String value) {
        if (value == null) return null;
        try {
            return NotificationChannel.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalTime.parse(value);
    }

    private static String format(LocalTime time) {
        return time != null ? time.format(TIME_FORMAT) : null;
    }

    private static String key(NotificationCategory category, NotificationChannel channel) {
        return category.name() + ":" + channel.name();
    }
}
