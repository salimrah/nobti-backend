package ma.noba.shared.domain.port;

import ma.noba.shared.application.dto.PagedResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Read and remediation surface for the admin money screens. */
public interface AdminPaymentPort {

    PagedResponse<TransactionRow> searchTransactions(String status, int page, int size);

    PagedResponse<PayoutRow> searchPayouts(String status, int page, int size);

    Optional<TransactionRow> findTransaction(UUID transactionId);

    void refund(UUID transactionId);

    record TransactionRow(
            UUID id,
            UUID userId,
            UUID providerId,
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

    record PayoutRow(
            UUID id,
            UUID providerId,
            BigDecimal amount,
            String currency,
            String status,
            String transferId,
            Instant createdAt,
            Instant paidAt) {
    }
}
