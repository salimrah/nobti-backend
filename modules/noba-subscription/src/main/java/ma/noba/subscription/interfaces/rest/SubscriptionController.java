package ma.noba.subscription.interfaces.rest;

import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.shared.domain.user.UserPrincipal;
import ma.noba.subscription.application.dto.SubscriptionResponse;
import ma.noba.subscription.application.dto.TierInfoResponse;
import ma.noba.subscription.application.dto.UpdateSubscriptionRequest;
import ma.noba.subscription.application.service.SubscriptionApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionApplicationService subscriptionService;
    private final ProviderLookupPort providerLookupPort;

    public SubscriptionController(SubscriptionApplicationService subscriptionService,
                                  ProviderLookupPort providerLookupPort) {
        this.subscriptionService = subscriptionService;
        this.providerLookupPort = providerLookupPort;
    }

    @PostMapping("/self")
    public ResponseEntity<SubscriptionResponse> createMySubscription(@AuthenticationPrincipal UserPrincipal user) {
        // The tier is no longer client-selectable: a new subscription always starts on FREE.
        SubscriptionResponse response = subscriptionService.createSubscription(requireProviderId(user));
        return ResponseEntity.created(URI.create("/api/v1/subscriptions/self"))
                .body(response);
    }

    @GetMapping("/self")
    public ResponseEntity<SubscriptionResponse> getMySubscription(@AuthenticationPrincipal UserPrincipal user) {
        SubscriptionResponse response = subscriptionService.getOrCreateDefault(requireProviderId(user));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/self")
    public ResponseEntity<SubscriptionResponse> updateMySubscription(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody(required = false) UpdateSubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscription(requireProviderId(user), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/self")
    public ResponseEntity<Void> cancelMySubscription(@AuthenticationPrincipal UserPrincipal user) {
        subscriptionService.cancelSubscription(requireProviderId(user));
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

    /**
     * Subscriptions are a provider concept and are keyed by the provider entity id (distinct from
     * the owner's user id). This keeps {@code /self} consistent with {@code /providers/{providerId}}.
     */
    private UUID requireProviderId(UserPrincipal user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return providerLookupPort.findProviderIdByUserId(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
