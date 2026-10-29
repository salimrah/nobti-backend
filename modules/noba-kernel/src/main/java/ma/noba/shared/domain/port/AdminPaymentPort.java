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

    Optional<TransactionDetail> findTransactionDetail(UUID transactionId);

    Optional<PayoutRow> findPayout(UUID payoutId);

    void refund(UUID transactionId);

    /** Refunds all or part of a transaction; null amount means "everything still refundable". */
    void refund(UUID transactionId, BigDecimal amount, String reason);

    /** Re-attempts a payout that previously failed. */
    void retryPayout(UUID payoutId);

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

    record TransactionDetail(
            UUID id,
            UUID userId,
            UUID providerId,
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
