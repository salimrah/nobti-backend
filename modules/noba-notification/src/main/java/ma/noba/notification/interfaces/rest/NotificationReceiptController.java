package ma.noba.notification.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.service.NotificationReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Provider callbacks for delivery receipts. Restricted to API-key clients
 * ({@code X-API-Key} with write permission), i.e. the notification provider integration.
 */
@RestController
@RequestMapping("/api/v1/notifications/receipts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('API')")
public class NotificationReceiptController {

    private final NotificationReceiptService receipts;

    @PostMapping
    public ResponseEntity<Map<String, String>> record(@Valid @RequestBody ReceiptRequest request) {
        NotificationReceiptService.ReceiptStatus status;
        try {
            status = NotificationReceiptService.ReceiptStatus.valueOf(request.status().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown status: " + request.status()));
        }
        return receipts.record(new NotificationReceiptService.Receipt(
                        request.deliveryId(),
                        request.providerMessageId(),
                        status,
                        request.occurredAt(),
                        request.errorCode(),
                        request.errorMessage()))
                .map(result -> ResponseEntity.ok(Map.of(
                        "deliveryId", result.deliveryId().toString(),
                        "status", result.status().name())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ReceiptRequest(
            UUID deliveryId,
            String providerMessageId,
            @NotBlank String status,
            Instant occurredAt,
            String errorCode,
            String errorMessage) {
    }
}
