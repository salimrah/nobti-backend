package ma.noba.statement.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read contract owned by the statement module for referrals. */
public interface StatementReferralSource {

    List<ReferralLine> findReferrals(UUID referrerId);

    record ReferralLine(Instant createdAt, String status, UUID id) {
    }
}
