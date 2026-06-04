package ma.noba.user.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ServiceHistoryResponse(
        String id,
        String userId,
        String providerId,
        String providerName,
        String serviceId,
        String serviceName,
        Instant completedAt,
        BigDecimal price,
        Integer rating
) {}
