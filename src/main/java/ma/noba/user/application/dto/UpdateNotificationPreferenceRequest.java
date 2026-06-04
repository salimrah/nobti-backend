package ma.noba.user.application.dto;

public record UpdateNotificationPreferenceRequest(
        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean smsEnabled
) {}