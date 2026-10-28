package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.service.NotificationDigestService;
import ma.noba.notification.application.service.NotificationDispatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/** Manual job triggers for the operations console (same code paths as the schedulers). */
@RestController
@RequestMapping("/api/v1/admin/ops/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationJobsController {

    private final NotificationDispatchService dispatch;
    private final NotificationDigestService digests;

    @PostMapping("/notification-dispatch")
    public ResponseEntity<Map<String, Object>> runDispatch() {
        dispatch.dispatchDue();
        return ResponseEntity.accepted().body(Map.of(
                "job", "notification-dispatch",
                "triggeredAt", Instant.now().toString()));
    }

    @PostMapping("/notification-digest")
    public ResponseEntity<Map<String, Object>> runDigest() {
        int processed = digests.flushDue();
        return ResponseEntity.accepted().body(Map.of(
                "job", "notification-digest",
                "processed", processed,
                "triggeredAt", Instant.now().toString()));
    }
}
