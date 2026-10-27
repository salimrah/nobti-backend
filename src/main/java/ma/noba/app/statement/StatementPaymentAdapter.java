package ma.noba.app.statement;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.statement.domain.port.StatementPaymentSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Composition-root adapter: translates the payment module's entities into the statement's
 * primitive read model. Neither module depends on the other's internals.
 */
@Component
@RequiredArgsConstructor
public class StatementPaymentAdapter implements StatementPaymentSource {

    private final PaymentTransactionRepository paymentTransactions;

    @Override
    public List<PaymentLine> findPayments(UUID userId) {
        return paymentTransactions.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(tx -> new PaymentLine(
                        tx.getCreatedAt(),
                        tx.getStatus() == PaymentStatus.SUCCEEDED,
                        tx.getStatus() != null ? tx.getStatus().name() : null,
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getId()))
                .toList();
    }
}
