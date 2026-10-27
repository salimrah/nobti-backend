package ma.noba.app.statement;

import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.statement.domain.port.StatementGiftCardSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StatementGiftCardAdapter implements StatementGiftCardSource {

    private final GiftCardRepository giftCards;

    @Override
    public List<GiftCardLine> findGiftCards(UUID buyerId) {
        return giftCards.findByBuyerId(buyerId).stream()
                .map(card -> new GiftCardLine(card.getCreatedAt(), card.getAmount(), card.isRedeemed(), card.getId()))
                .toList();
    }
}
