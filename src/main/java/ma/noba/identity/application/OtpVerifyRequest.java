package ma.noba.identity.application;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record OtpVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank String code,
        UUID deviceId
) {}