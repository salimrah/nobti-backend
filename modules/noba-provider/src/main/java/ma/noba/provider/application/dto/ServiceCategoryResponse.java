package ma.noba.provider.application.dto;

import java.time.Duration;
import java.util.UUID;

public record ServiceCategoryResponse(
        UUID id,
        String name,
        Duration defaultDuration,
        String description
) {}
