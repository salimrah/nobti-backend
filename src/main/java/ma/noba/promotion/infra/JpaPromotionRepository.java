package ma.noba.promotion.infra;

import ma.noba.promotion.domain.model.Promotion;
import ma.noba.promotion.domain.port.PromotionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaPromotionRepository extends JpaRepository<Promotion, UUID>, PromotionRepository {
    List<Promotion> findByProviderId(UUID providerId);
    List<Promotion> findByProviderIdAndIsActive(UUID providerId, boolean isActive);

    @Query("SELECT p FROM Promotion p WHERE p.providerId = :pid AND p.isActive = true AND p.validFrom <= :now AND p.validTo >= :now AND (p.maxRedemptions IS NULL OR p.currentRedemptions < p.maxRedemptions)")
    List<Promotion> findValidPromotions(@Param("pid") UUID providerId, @Param("now") Instant now);
}
