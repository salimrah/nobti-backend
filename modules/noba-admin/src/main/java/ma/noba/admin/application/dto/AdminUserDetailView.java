package ma.noba.admin.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminUserDetailView(
        UUID id,
        String name,
        String email,
        String phone,
        String status,
        Set<String> roles,
        Instant createdAt) {
}
