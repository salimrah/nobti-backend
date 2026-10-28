package ma.noba.notification.application.dto;

import java.util.List;

public record NotificationPreferencesResponse(
        List<ChannelPreferenceDto> channels,
        String quietHoursStart,
        String quietHoursEnd,
        String timezone,
        String digest
) {
}
