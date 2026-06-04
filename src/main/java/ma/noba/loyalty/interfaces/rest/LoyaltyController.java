package ma.noba.loyalty.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.loyalty.application.dto.*;
import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyApplicationService loyaltyService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping("/me")
    public ResponseEntity<MyLoyaltyResponse> getMyLoyalty(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(loyaltyService.getMyLoyalty(userId));
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<LoyaltyResponse> getProviderLoyalty(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID providerId
    ) {
        return ResponseEntity.ok(loyaltyService.getProviderLoyalty(userId, providerId));
    }

    @GetMapping("/providers/self/rewards")
    public ResponseEntity<List<LoyaltyRewardResponse>> getMyRewards(@AuthenticationPrincipal UUID userId) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.ok(loyaltyService.getProviderRewards(providerId));
    }

    @PostMapping("/providers/self/rewards")
    public ResponseEntity<LoyaltyRewardResponse> createReward(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreateRewardRequest request
    ) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(loyaltyService.createReward(providerId, request));
    }

    @DeleteMapping("/providers/self/rewards/{rewardId}")
    public ResponseEntity<Void> deactivateReward(@PathVariable UUID rewardId) {
        loyaltyService.deactivateReward(rewardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rewards/{rewardId}/redeem")
    public ResponseEntity<RedeemRewardResponse> redeemReward(
        @AuthenticationPrincipal UUID userId,
        @PathVariable UUID rewardId
    ) {
        return ResponseEntity.ok(loyaltyService.redeemReward(userId, rewardId));
    }

    private UUID getProviderIdForUser(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
