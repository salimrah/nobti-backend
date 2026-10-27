package ma.noba.identity.application;

import jakarta.validation.constraints.NotNull;
import ma.noba.identity.domain.model.SocialProvider;

import java.util.UUID;

public record SocialAuthRequest(
        @NotNull SocialProvider provider,
        String idToken,
        String accessToken,
        UUID deviceId
) {}

