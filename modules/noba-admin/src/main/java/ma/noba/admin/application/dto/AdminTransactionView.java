package ma.noba.admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminTransactionView(
        UUID id,
        UUID userId,
        String clientName,
        UUID providerId,
        String providerName,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal netAmount,
        String currency,
        String status,
        String paymentIntentId,
        String failureMessage,
        Instant createdAt,
        Instant completedAt) {
}
