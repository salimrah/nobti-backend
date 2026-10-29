package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.UUID;

/** An operations account (holder of the ADMIN role). */
public record AdminAccountView(
        UUID id,
        String name,
        String email,
        String status,
        Instant createdAt) {
}
