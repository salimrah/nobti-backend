package ma.noba.payment.interfaces.rest;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.noba.config.StripeConfig;
import ma.noba.payment.application.service.PaymentApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/stripe/webhook")
@ConditionalOnBean(StripeConfig.class)
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeConfig stripeConfig;
    private final PaymentApplicationService paymentService;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        try {
            Event event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());

            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);
                    if (paymentIntent != null) {
                        String chargeId = paymentIntent.getLatestCharge();
                        paymentService.handlePaymentSuccess(paymentIntent.getId(), chargeId);
                        log.info("Payment succeeded: PI={}, Charge={}", paymentIntent.getId(), chargeId);
                    }
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject()
                        .orElse(null);
                    if (paymentIntent != null) {
                        String failureMessage = paymentIntent.getLastPaymentError() != null
                            ? paymentIntent.getLastPaymentError().getMessage()
                            : "Unknown error";
                        paymentService.handlePaymentFailure(paymentIntent.getId(), failureMessage);
                        log.warn("Payment failed: PI={}, reason={}", paymentIntent.getId(), failureMessage);
                    }
                }
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }

            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }
    }
}
