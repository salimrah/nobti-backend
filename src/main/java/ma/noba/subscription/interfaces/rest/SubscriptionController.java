package ma.noba.subscription.interfaces.rest;

import ma.noba.shared.domain.user.UserPrincipal;
import ma.noba.subscription.application.dto.SubscriptionResponse;
import ma.noba.subscription.application.dto.TierInfoResponse;
import ma.noba.subscription.application.dto.UpdateSubscriptionRequest;
import ma.noba.subscription.application.service.SubscriptionApplicationService;
import ma.noba.subscription.domain.model.SubscriptionTier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionApplicationService subscriptionService;

    public SubscriptionController(SubscriptionApplicationService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/self")
    public ResponseEntity<SubscriptionResponse> createMySubscription(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "FREE") SubscriptionTier tier) {
        SubscriptionResponse response = subscriptionService.createSubscription(user.id(), tier);
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/self"))
                .body(response);
    }

    @GetMapping("/self")
    public ResponseEntity<SubscriptionResponse> getMySubscription(@AuthenticationPrincipal UserPrincipal user) {
        SubscriptionResponse response = subscriptionService.getOrCreateDefault(user.id());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/self")
    public ResponseEntity<SubscriptionResponse> updateMySubscription(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody UpdateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscription(user.id(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/self")
    public ResponseEntity<Void> cancelMySubscription(@AuthenticationPrincipal UserPrincipal user) {
        subscriptionService.cancelSubscription(user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tiers")
    public ResponseEntity<List<TierInfoResponse>> getTierInfo() {
        return ResponseEntity.ok(subscriptionService.getTierInfo());
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<SubscriptionResponse> getProviderSubscription(@PathVariable UUID providerId) {
        SubscriptionResponse response = subscriptionService.getOrCreateDefault(providerId);
        return ResponseEntity.ok(response);
    }
}
