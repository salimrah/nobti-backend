package ma.noba.statement.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single financial/loyalty movement in a user's statement. Money entries populate {@code amount}
 * and {@code currency}; loyalty entries populate {@code points}.
 */
public record StatementEntry(
        Instant at,
        String type,
        String description,
        BigDecimal amount,
        String currency,
        Integer points,
        String status,
        String referenceId
) {}
