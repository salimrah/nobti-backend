package ma.noba.packagegift.domain.port;

import ma.noba.packagegift.domain.model.GiftCard;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GiftCardRepository {
    GiftCard save(GiftCard giftCard);
    Optional<GiftCard> findById(UUID id);
    Optional<GiftCard> findByCode(String code);
    boolean existsByPaymentTransactionId(UUID paymentTransactionId);
    List<GiftCard> findByBuyerId(UUID buyerId);
    List<GiftCard> findByProviderId(UUID providerId);
}
