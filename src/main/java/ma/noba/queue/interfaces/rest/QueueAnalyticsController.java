package ma.noba.queue.interfaces.rest;

import ma.noba.queue.application.dto.QueueAnalyticsResponse;
import ma.noba.queue.application.service.QueueAnalyticsService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
public class QueueAnalyticsController {

    private final QueueAnalyticsService analyticsService;

    public QueueAnalyticsController(QueueAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/providers/self/analytics/today")
    public ResponseEntity<QueueAnalyticsResponse> getTodayAnalytics(@AuthenticationPrincipal UserPrincipal user) {
        QueueAnalyticsResponse response = analyticsService.getTodayAnalytics(user.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/self/analytics/week")
    public ResponseEntity<QueueAnalyticsResponse> getWeekAnalytics(@AuthenticationPrincipal UserPrincipal user) {
        QueueAnalyticsResponse response = analyticsService.getWeekAnalytics(user.id());
        return ResponseEntity.ok(response);
    }
}
