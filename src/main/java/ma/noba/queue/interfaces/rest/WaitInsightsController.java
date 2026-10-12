package ma.noba.queue.interfaces.rest;

import ma.noba.queue.application.dto.WaitInsightsResponse;
import ma.noba.queue.application.service.WaitInsightsService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
public class WaitInsightsController {

    private final WaitInsightsService waitInsightsService;
    private final ProviderLookupPort providerLookupPort;

    public WaitInsightsController(WaitInsightsService waitInsightsService, ProviderLookupPort providerLookupPort) {
        this.waitInsightsService = waitInsightsService;
        this.providerLookupPort = providerLookupPort;
    }

    @GetMapping("/{providerId}/wait-insights")
    public ResponseEntity<WaitInsightsResponse> getWaitInsights(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        // The path variable is the provider entity id; compare it against the caller's provider id.
        UUID callerProviderId = (user == null || !user.isProvider())
                ? null
                : providerLookupPort.findProviderIdByUserId(user.id()).orElse(null);
        if (callerProviderId == null || !callerProviderId.equals(providerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(waitInsightsService.getWaitInsights(providerId));
    }
}
