package ma.noba.admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminTransactionDetailView(
        UUID id,
        UUID userId,
        String clientName,
        UUID providerId,
        String providerName,
        UUID serviceId,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal netAmount,
        BigDecimal refundedAmount,
        String refundReason,
        String currency,
        String status,
        String paymentIntentId,
        String chargeId,
        String transferId,
        String failureMessage,
        Instant createdAt,
        Instant completedAt) {
}
