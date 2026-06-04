package ma.noba.queue.interfaces.rest;

import ma.noba.queue.application.dto.WaitInsightsResponse;
import ma.noba.queue.application.service.WaitInsightsService;
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

    public WaitInsightsController(WaitInsightsService waitInsightsService) {
        this.waitInsightsService = waitInsightsService;
    }

    @GetMapping("/{providerId}/wait-insights")
    public ResponseEntity<WaitInsightsResponse> getWaitInsights(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        if (!user.isProvider() || !user.id().equals(providerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(waitInsightsService.getWaitInsights(providerId));
    }
}
