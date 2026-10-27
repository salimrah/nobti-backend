package ma.noba.statement.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read contract owned by the statement module for loyalty point movements. */
public interface StatementLoyaltySource {

    List<LoyaltyLine> findLoyaltyMovements(UUID userId);

    record LoyaltyLine(Instant createdAt, int points, String reason, UUID referenceId) {
    }
}
