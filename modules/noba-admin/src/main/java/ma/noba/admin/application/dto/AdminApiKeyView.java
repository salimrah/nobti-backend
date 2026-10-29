package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminApiKeyView(
        UUID id,
        String owner,
        String permissions,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt) {
}
