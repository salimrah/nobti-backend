package ma.noba.user.application.dto;

import ma.noba.shared.domain.model.UserStatus;

import java.util.Set;

public record UserResponse(
        String id,
        String phone,
        String email,
        UserStatus status,
        String firstName,
        String lastName,
        String profilePictureUrl,
        Set<String> roles
) {}
