package ma.noba.provider.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Duration;

public record AddServiceRequest(
        @NotBlank(message = "Service name is required")
        String name,
        @NotNull(message = "Duration is required")
        Duration duration,
        @NotNull(message = "Price is required")
        BigDecimal price,
        String category
) {}
