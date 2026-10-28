package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.NotificationAnalyticsResponse;
import ma.noba.notification.application.service.NotificationAnalyticsService;
import ma.noba.notification.application.service.NotificationEventExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/notifications/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NotificationAnalyticsController {

    private final NotificationAnalyticsService analytics;
    private final NotificationEventExportService events;

    @GetMapping
    public ResponseEntity<NotificationAnalyticsResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(analytics.summary(from, to));
    }

    /**
     * Pseudonymized event log for the warehouse. Keyset paginated: pass the previous
     * {@code X-Next-After} value as {@code after} until the response has no rows.
     */
    @GetMapping(value = "/events", produces = "text/csv")
    public ResponseEntity<String> events(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") long after,
            @RequestParam(defaultValue = "1000") int limit) {
        NotificationEventExportService.ExportPage page = events.events(from, to, after, limit);
        return ResponseEntity.ok()
                .header("X-Next-After", String.valueOf(page.nextAfter()))
                .header("X-Page-Rows", String.valueOf(page.rows()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notification-events.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(page.csv());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"notifications.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(analytics.csv(from, to));
    }
}
