package ma.noba.payment.interfaces.rest;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.noba.config.StripeConfig;
import ma.noba.payment.application.service.PaymentApplicationService;
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
    private final PaymentApplicationService paymentService;
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

        PaymentIntent paymentIntent = event.getDataObjectDeserializer().getObject()
                .filter(obj -> obj instanceof PaymentIntent)
                .map(obj -> (PaymentIntent) obj)
                .orElse(null);
        String paymentIntentId = paymentIntent != null ? paymentIntent.getId() : null;
        String chargeId = paymentIntent != null ? paymentIntent.getLatestCharge() : null;
        String failureMessage = paymentIntent != null && paymentIntent.getLastPaymentError() != null
                ? paymentIntent.getLastPaymentError().getMessage() : null;

        PaymentWebhookEvent record = webhookLog.record(PROVIDER, event.getId(), event.getType(),
                paymentIntentId, chargeId, failureMessage, payload);

        // Stripe retries deliveries: an event already handled is acknowledged without re-running it.
        if ("PROCESSED".equals(record.getStatus())) {
            log.debug("Ignoring already processed Stripe event {}", event.getId());
            return ResponseEntity.ok("");
        }

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    if (paymentIntent != null) {
                        paymentService.handlePaymentSuccess(paymentIntent.getId(), chargeId);
                        log.info("Payment succeeded: PI={}, Charge={}", paymentIntent.getId(), chargeId);
                    }
                }
                case "payment_intent.payment_failed" -> {
                    if (paymentIntent != null) {
                        paymentService.handlePaymentFailure(paymentIntent.getId(), failureMessage);
                        log.warn("Payment failed: PI={}, reason={}", paymentIntent.getId(), failureMessage);
                    }
                }
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }
            webhookLog.markProcessed(record.getId());
            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed for {}", event.getId(), e);
            webhookLog.markFailed(record.getId(), e.getMessage());
            // Return 500 so Stripe retries; the event stays replayable from the console.
            return ResponseEntity.internalServerError().body("Processing failed");
        }
    }
}
