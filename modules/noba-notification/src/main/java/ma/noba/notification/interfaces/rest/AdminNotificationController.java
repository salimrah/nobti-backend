package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.AdminDeliveryView;
import ma.noba.notification.application.service.NotificationDeliveryAdminService;
import ma.noba.shared.application.dto.PagedResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Delivery log and retries for the operations console. */
@RestController
@RequestMapping("/api/v1/admin/notifications/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final NotificationDeliveryAdminService deliveries;

    @GetMapping
    public ResponseEntity<PagedResponse<AdminDeliveryView>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deliveries.search(status, channel, page, size));
    }

    @PostMapping("/{deliveryId}/retry")
    public ResponseEntity<AdminDeliveryView> retry(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(deliveries.retry(deliveryId));
    }
}
