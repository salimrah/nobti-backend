package ma.noba.queue.interfaces.rest;

import ma.noba.queue.application.dto.QueueAnalyticsResponse;
import ma.noba.queue.application.service.QueueAnalyticsService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueAnalyticsController {

    private final QueueAnalyticsService analyticsService;
    private final ProviderLookupPort providerLookupPort;

    public QueueAnalyticsController(QueueAnalyticsService analyticsService, ProviderLookupPort providerLookupPort) {
        this.analyticsService = analyticsService;
        this.providerLookupPort = providerLookupPort;
    }

    @GetMapping("/providers/self/analytics/today")
    public ResponseEntity<QueueAnalyticsResponse> getTodayAnalytics(@AuthenticationPrincipal UserPrincipal user) {
        QueueAnalyticsResponse response = analyticsService.getTodayAnalytics(requireProviderId(user));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/self/analytics/week")
    public ResponseEntity<QueueAnalyticsResponse> getWeekAnalytics(@AuthenticationPrincipal UserPrincipal user) {
        QueueAnalyticsResponse response = analyticsService.getWeekAnalytics(requireProviderId(user));
        return ResponseEntity.ok(response);
    }

    private UUID requireProviderId(UserPrincipal user) {
        if (user == null || !user.isProvider()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return providerLookupPort.findProviderIdByUserId(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No provider profile"));
    }
}
