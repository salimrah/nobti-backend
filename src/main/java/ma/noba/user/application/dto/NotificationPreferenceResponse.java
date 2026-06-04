package ma.noba.user.application.dto;

public record NotificationPreferenceResponse(
        boolean pushEnabled,
        boolean emailEnabled,
        boolean smsEnabled
) {}