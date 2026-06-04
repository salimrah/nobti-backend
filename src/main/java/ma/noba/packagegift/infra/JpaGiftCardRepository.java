package ma.noba.packagegift.infra;

import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaGiftCardRepository extends JpaRepository<GiftCard, UUID>, GiftCardRepository {

    Optional<GiftCard> findByCode(String code);

    List<GiftCard> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    List<GiftCard> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
}
