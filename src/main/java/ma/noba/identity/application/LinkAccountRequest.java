package ma.noba.identity.application;

import jakarta.validation.constraints.NotNull;
import ma.noba.identity.domain.model.SocialProvider;

public record LinkAccountRequest(
        @NotNull SocialProvider provider,
        String idToken,
        String accessToken,
        String email
) {}
