package ma.noba.payment.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.application.dto.AdminWebhookView;
import ma.noba.payment.application.service.PaymentWebhookLogService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminAuditPort;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Webhook inspection and replay for operations. */
@RestController
@RequestMapping("/api/v1/admin/payments/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentWebhookController {

    private final PaymentWebhookLogService webhooks;
    private final ObjectProvider<AdminAuditPort> audit;

    @GetMapping
    public ResponseEntity<PagedResponse<AdminWebhookView>> search(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<ma.noba.payment.domain.model.PaymentWebhookEvent> rows =
                webhooks.search(status, page, size);
        return ResponseEntity.ok(PagedResponse.of(
                rows.content().stream().map(event -> AdminWebhookView.of(event, false)).toList(),
                rows.page(), rows.size(), rows.totalElements()));
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<AdminWebhookView> detail(@PathVariable UUID webhookId) {
        return webhooks.find(webhookId)
                .map(event -> ResponseEntity.ok(AdminWebhookView.of(event, true)))
                .orElseThrow(() -> new NoSuchElementException("Webhook event not found"));
    }

    @PostMapping("/{webhookId}/replay")
    public ResponseEntity<AdminWebhookView> replay(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable UUID webhookId) {
        var event = webhooks.replay(webhookId);
        AdminAuditPort auditor = audit.getIfAvailable();
        if (auditor != null) {
            auditor.record(principal.id(), principal.email(), "WEBHOOK_REPLAYED", "WEBHOOK", webhookId,
                    Map.of("eventType", String.valueOf(event.getEventType()),
                            "status", event.getStatus(),
                            "replayCount", event.getReplayCount()));
        }
        return ResponseEntity.ok(AdminWebhookView.of(event, false));
    }
}
