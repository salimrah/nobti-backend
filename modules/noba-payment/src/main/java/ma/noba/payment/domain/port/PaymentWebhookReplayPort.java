package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentWebhookEvent;

/** Re-applies a stored provider webhook, implemented by the provider adapter (Stripe today). */
public interface PaymentWebhookReplayPort {

    void replay(PaymentWebhookEvent event);
}
