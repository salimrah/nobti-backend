package ma.noba.admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPayoutView(
        UUID id,
        UUID providerId,
        String providerName,
        BigDecimal amount,
        String currency,
        String status,
        String transferId,
        Instant createdAt,
        Instant paidAt) {
}
