package ma.noba.provider.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProviderRequest(
        @NotBlank(message = "Provider name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,
        String description,
        String category,
        String address,
        Double latitude,
        Double longitude,
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName
) {}
