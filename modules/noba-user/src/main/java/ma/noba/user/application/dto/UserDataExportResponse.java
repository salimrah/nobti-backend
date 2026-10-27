package ma.noba.user.application.dto;

import ma.noba.shared.domain.model.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserDataExportResponse(
        UUID userId,
        String email,
        String phone,
        UserStatus status,
        String firstName,
        String lastName,
        String profilePictureUrl,
        Set<String> roles,
        Instant createdAt
) {}
