package ma.noba.payment.interfaces.rest;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.noba.config.StripeConfig;
import ma.noba.payment.application.service.PaymentWebhookLogService;
import ma.noba.payment.domain.model.PaymentWebhookEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/webhook")
@ConditionalOnBean(StripeConfig.class)
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final String PROVIDER = "stripe";

    private final StripeConfig stripeConfig;
    private final PaymentWebhookLogService webhookLog;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            webhookLog.recordRejected(PROVIDER, null, null, payload, "Invalid signature: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        // Keep the display columns useful; the handler re-reads the same object from the payload.
        PaymentIntent intent = event.getDataObjectDeserializer().getObject()
                .filter(object -> object instanceof PaymentIntent)
                .map(object -> (PaymentIntent) object)
                .orElse(null);
        PaymentWebhookEvent record = webhookLog.record(PROVIDER, event.getId(), event.getType(),
                intent != null ? intent.getId() : null,
                intent != null ? intent.getLatestCharge() : null,
                intent != null && intent.getLastPaymentError() != null
                        ? intent.getLastPaymentError().getMessage() : null,
                payload);

        // Stripe retries deliveries: an event already handled is acknowledged without re-running it.
        if ("PROCESSED".equals(record.getStatus())) {
            log.debug("Ignoring already processed Stripe event {}", event.getId());
            return ResponseEntity.ok("");
        }

        PaymentWebhookEvent processed = webhookLog.process(record.getId());
        if ("PROCESSED".equals(processed.getStatus())) {
            return ResponseEntity.ok("");
        }
        log.error("Stripe webhook processing failed for {}: {}", event.getId(), processed.getErrorMessage());
        // Return 500 so Stripe retries; the event stays replayable from the console.
        return ResponseEntity.internalServerError().body("Processing failed");
    }
}
