package ma.noba.provider.application.dto;

public record ProviderSettingsResponse(
        String providerId,
        int maxPartySize,
        boolean walkInsEnabled,
        boolean appointmentsEnabled,
        int noShowGraceMinutes,
        int cancellationWindowMinutes,
        boolean autoFillEnabled,
        int loyaltyPointsPerVisit,
        String notificationLocale
) {}
