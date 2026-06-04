package ma.noba.user.application.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String fcmToken,
        String platform
) {}
