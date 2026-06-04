package ma.noba.promotion.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.promotion.application.dto.CreatePromotionRequest;
import ma.noba.promotion.application.dto.PromotionResponse;
import ma.noba.promotion.application.service.PromotionApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionApplicationService promotionService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping("/providers/{providerId}/promotions")
    public ResponseEntity<List<PromotionResponse>> getPromotions(@PathVariable UUID providerId) {
        return ResponseEntity.ok(promotionService.getActivePromotions(providerId));
    }

    @GetMapping("/providers/self/promotions")
    public ResponseEntity<List<PromotionResponse>> getMyPromotions(@AuthenticationPrincipal UUID userId) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.ok(promotionService.getProviderPromotions(providerId));
    }

    @PostMapping("/providers/self/promotions")
    public ResponseEntity<PromotionResponse> createPromotion(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreatePromotionRequest request
    ) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(promotionService.createPromotion(providerId, request));
    }

    @DeleteMapping("/promotions/{promotionId}")
    public ResponseEntity<Void> deactivatePromotion(@PathVariable UUID promotionId) {
        promotionService.deactivatePromotion(promotionId);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderIdForUser(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
