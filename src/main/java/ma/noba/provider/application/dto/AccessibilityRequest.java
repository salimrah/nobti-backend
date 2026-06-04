package ma.noba.provider.application.dto;

public record AccessibilityRequest(
        boolean wheelchairAccessible,
        boolean signLanguage,
        boolean sensoryFriendly
) {}
