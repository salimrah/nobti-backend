package ma.noba.admin.application.dto;

import java.time.Instant;

public record CreateApiKeyRequest(String owner, String permissions, Instant expiresAt) {
}
