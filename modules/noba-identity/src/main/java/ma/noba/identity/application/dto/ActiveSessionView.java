package ma.noba.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

/** A user's active session, exposed to the auth API without leaking the token service internals. */
public record ActiveSessionView(UUID id, Instant createdAt, Instant expiresAt, Instant lastUsedAt) {
}
