package ma.noba.notification.application.dto;

import java.util.List;

public record UpdateNotificationPreferencesRequest(
        List<ChannelPreferenceDto> channels,
        String quietHoursStart,
        String quietHoursEnd,
        String timezone,
        String digest
) {
}
