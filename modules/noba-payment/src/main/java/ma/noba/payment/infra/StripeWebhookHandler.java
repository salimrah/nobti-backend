package ma.noba.payment.infra;

import com.stripe.model.Account;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Transfer;
import com.stripe.net.ApiResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.noba.config.StripeConfig;
import ma.noba.payment.application.service.PaymentApplicationService;
import ma.noba.payment.domain.model.PaymentWebhookEvent;
import ma.noba.payment.domain.port.PaymentWebhookReplayPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Single place where Stripe events are applied, used both for live deliveries and for replays of
 * events previously stored by the webhook log.
 */
@Slf4j
@Service
@ConditionalOnBean(StripeConfig.class)
@RequiredArgsConstructor
public class StripeWebhookHandler implements PaymentWebhookReplayPort {

    private final PaymentApplicationService payments;

    public void handle(Event event) {
        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = object(event, PaymentIntent.class);
                if (intent != null) {
                    payments.handlePaymentSuccess(intent.getId(), intent.getLatestCharge());
                    log.info("Payment succeeded: PI={}, Charge={}", intent.getId(), intent.getLatestCharge());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = object(event, PaymentIntent.class);
                if (intent != null) {
                    String message = intent.getLastPaymentError() != null
                            ? intent.getLastPaymentError().getMessage() : "Unknown error";
                    payments.handlePaymentFailure(intent.getId(), message);
                    log.warn("Payment failed: PI={}, reason={}", intent.getId(), message);
                }
            }
            case "transfer.created", "transfer.updated", "transfer.reversed" -> {
                Transfer transfer = object(event, Transfer.class);
                if (transfer != null) {
                    boolean reversed = "transfer.reversed".equals(event.getType())
                            || Boolean.TRUE.equals(transfer.getReversed());
                    payments.handleTransferEvent(transfer.getId(), reversed);
                }
            }
            case "charge.refunded" -> {
                // Refunds issued from the Stripe dashboard are logged but not auto-applied: reversing
                // the provider transfer is a money decision that stays with an operator.
                Charge charge = object(event, Charge.class);
                if (charge != null) {
                    log.warn("Charge {} was refunded outside the console (amount_refunded={})",
                            charge.getId(), charge.getAmountRefunded());
                }
            }
            case "account.updated" -> {
                Account account = object(event, Account.class);
                if (account != null) {
                    payments.handleAccountUpdated(account.getId(), Boolean.TRUE.equals(account.getPayoutsEnabled()));
                    log.info("Connected account {} updated (payouts_enabled={})",
                            account.getId(), account.getPayoutsEnabled());
                }
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    @Override
    public void replay(PaymentWebhookEvent event) {
        if (event.getPayload() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("This event carries no replayable payload");
        }
        Event parsed = ApiResource.GSON.fromJson(event.getPayload(), Event.class);
        if (parsed == null || parsed.getType() == null) {
            throw new IllegalArgumentException("Stored payload is not a Stripe event");
        }
        handle(parsed);
    }

    private static <T extends StripeObject> T object(Event event, Class<T> type) {
        return event.getDataObjectDeserializer().getObject()
                .filter(type::isInstance)
                .map(type::cast)
                .orElse(null);
    }
}
