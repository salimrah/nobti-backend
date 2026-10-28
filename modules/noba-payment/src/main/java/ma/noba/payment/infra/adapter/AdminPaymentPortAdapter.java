package ma.noba.payment.infra.adapter;

import ma.noba.payment.application.service.PaymentApplicationService;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.model.PayoutStatus;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.payment.domain.port.PayoutRepository;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminPaymentPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminPaymentPortAdapter implements AdminPaymentPort {

    private final PaymentTransactionRepository transactions;
    private final PayoutRepository payouts;
    private final PaymentApplicationService paymentService;

    public AdminPaymentPortAdapter(PaymentTransactionRepository transactions, PayoutRepository payouts,
                                   PaymentApplicationService paymentService) {
        this.transactions = transactions;
        this.payouts = payouts;
        this.paymentService = paymentService;
    }

    @Override
    public PagedResponse<TransactionRow> searchTransactions(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        PaymentStatus parsed = parse(status, PaymentStatus.class);
        Page<PaymentTransaction> result = parsed == null
                ? transactions.findAllByOrderByCreatedAtDesc(pageable)
                : transactions.findByStatusOrderByCreatedAtDesc(parsed, pageable);
        return PagedResponse.of(
                result.getContent().stream().map(AdminPaymentPortAdapter::toRow).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public PagedResponse<PayoutRow> searchPayouts(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        PayoutStatus parsed = parse(status, PayoutStatus.class);
        Page<Payout> result = parsed == null
                ? payouts.findAllByOrderByCreatedAtDesc(pageable)
                : payouts.findByStatusOrderByCreatedAtDesc(parsed, pageable);
        return PagedResponse.of(
                result.getContent().stream().map(AdminPaymentPortAdapter::toRow).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Override
    public Optional<TransactionRow> findTransaction(UUID transactionId) {
        return transactions.findById(transactionId).map(AdminPaymentPortAdapter::toRow);
    }

    @Override
    public void refund(UUID transactionId) {
        paymentService.processRefund(transactionId);
    }

    private static TransactionRow toRow(PaymentTransaction transaction) {
        return new TransactionRow(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getProviderId(),
                transaction.getAmount(),
                transaction.getFee(),
                transaction.getNetAmount(),
                transaction.getCurrency(),
                transaction.getStatus().name(),
                transaction.getStripePaymentIntentId(),
                transaction.getFailureMessage(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt());
    }

    private static PayoutRow toRow(Payout payout) {
        return new PayoutRow(
                payout.getId(),
                payout.getProviderId(),
                payout.getAmount(),
                payout.getCurrency(),
                payout.getStatus().name(),
                payout.getStripeTransferId(),
                payout.getCreatedAt(),
                payout.getPaidAt());
    }

    private static <E extends Enum<E>> E parse(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
