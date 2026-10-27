package ma.noba.shared.interfaces.rest;

import ma.noba.shared.domain.webhook.WebhookSubscription;
import ma.noba.shared.domain.port.WebhookSubscriptionPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Manage webhook subscriptions for domain events")
public class WebhookController {

    private final WebhookSubscriptionPort repository;

    public WebhookController(WebhookSubscriptionPort repository) {
        this.repository = repository;
    }

    @Operation(summary = "List all webhook subscriptions")
    @GetMapping
    public ResponseEntity<List<WebhookSubscription>> listSubscriptions() {
        return ResponseEntity.ok(repository.findActiveSubscriptions());
    }

    @Operation(summary = "Create a webhook subscription")
    @PostMapping
    public ResponseEntity<WebhookSubscription> createSubscription(
            @RequestBody CreateWebhookRequest request) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setUrl(request.url());
        subscription.setEventType(request.eventType());
        subscription.setSecret(request.secret());
        subscription.setActive(true);

        WebhookSubscription saved = repository.saveSubscription(subscription);
        return ResponseEntity.created(URI.create("/api/v1/webhooks/" + saved.getId()))
                .body(saved);
    }

    @Operation(summary = "Delete a webhook subscription")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID id) {
        repository.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Toggle a webhook subscription")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<WebhookSubscription> toggleSubscription(@PathVariable UUID id) {
        WebhookSubscription subscription = repository.findSubscription(id)
                .orElseThrow(() -> new RuntimeException("Webhook not found"));
        subscription.setActive(!subscription.isActive());
        return ResponseEntity.ok(repository.saveSubscription(subscription));
    }

    public record CreateWebhookRequest(String url, String eventType, String secret) {}
}
