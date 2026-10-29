package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/** The plaintext key is returned once, at creation time only. */
public record CreatedApiKeyResponse(
        UUID id,
        String owner,
        String key,
        String permissions,
        Instant expiresAt) {
}
