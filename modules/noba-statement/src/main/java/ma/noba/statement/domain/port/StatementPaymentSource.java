package ma.noba.statement.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read contract owned by the statement module: the payment module implements it without the
 * statement ever seeing a JPA entity. View records are intentionally primitive.
 */
public interface StatementPaymentSource {

    List<PaymentLine> findPayments(UUID userId);

    record PaymentLine(
            Instant createdAt,
            boolean succeeded,
            String status,
            BigDecimal amount,
            String currency,
            UUID id
    ) {
    }
}
