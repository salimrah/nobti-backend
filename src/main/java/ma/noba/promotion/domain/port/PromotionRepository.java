package ma.noba.promotion.domain.port;

import ma.noba.promotion.domain.model.Promotion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository {
    Promotion save(Promotion promotion);
    Optional<Promotion> findById(UUID id);
    List<Promotion> findByProviderId(UUID providerId);
    List<Promotion> findByProviderIdAndIsActive(UUID providerId, boolean isActive);
    List<Promotion> findValidPromotions(UUID providerId, Instant now);
    void delete(Promotion promotion);
}
