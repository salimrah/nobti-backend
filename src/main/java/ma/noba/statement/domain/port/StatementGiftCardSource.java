package ma.noba.statement.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read contract owned by the statement module for gift-card purchases. */
public interface StatementGiftCardSource {

    List<GiftCardLine> findGiftCards(UUID buyerId);

    record GiftCardLine(Instant createdAt, BigDecimal amount, boolean redeemed, UUID id) {
    }
}
