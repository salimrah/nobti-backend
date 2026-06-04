package ma.noba.provider.application.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public record ProviderServiceResponse(
        UUID id,
        String name,
        Duration duration,
        BigDecimal price,
        String category,
        boolean active
) {}
