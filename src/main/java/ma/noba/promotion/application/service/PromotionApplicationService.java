package ma.noba.promotion.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.promotion.application.dto.CreatePromotionRequest;
import ma.noba.promotion.application.dto.PromotionResponse;
import ma.noba.promotion.domain.model.Promotion;
import ma.noba.promotion.domain.port.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionApplicationService {

    private final PromotionRepository promotionRepository;

    public List<PromotionResponse> getProviderPromotions(UUID providerId) {
        return promotionRepository.findByProviderId(providerId).stream()
            .map(PromotionApplicationService::toResponse)
            .toList();
    }

    public List<PromotionResponse> getActivePromotions(UUID providerId) {
        return promotionRepository.findValidPromotions(providerId, Instant.now()).stream()
            .map(PromotionApplicationService::toResponse)
            .toList();
    }

    @Transactional
    public PromotionResponse createPromotion(UUID providerId, CreatePromotionRequest request) {
        List<UUID> serviceIds = request.getServiceIds() != null
            ? request.getServiceIds().stream().map(UUID::fromString).toList()
            : List.of();

        Promotion promotion = new Promotion(
            providerId, request.getTitle(), request.getDescription(),
            request.getDiscountPercent(), request.getValidFrom(), request.getValidTo(),
            request.getMaxRedemptions(), serviceIds
        );
        promotion = promotionRepository.save(promotion);
        return toResponse(promotion);
    }

    @Transactional
    public void deactivatePromotion(UUID promotionId, UUID providerId) {
        Promotion promotion = promotionRepository.findById(promotionId)
            .orElseThrow(() -> new IllegalArgumentException("Promotion not found: " + promotionId));
        if (!promotion.getProviderId().equals(providerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not the provider of this promotion");
        }
        promotion.deactivate();
        promotionRepository.save(promotion);
    }

    public static PromotionResponse toResponse(Promotion p) {
        return new PromotionResponse(
            p.getId().toString(), p.getProviderId().toString(),
            p.getTitle(), p.getDescription(), p.getDiscountPercent(),
            p.getValidFrom(), p.getValidTo(),
            p.getMaxRedemptions(), p.getCurrentRedemptions(),
            p.isActive(), p.isValid(),
            p.getServiceIds() != null ? p.getServiceIds().stream().map(UUID::toString).toList() : List.of(),
            p.getCreatedAt()
        );
    }
}
