package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

public interface StripePaymentPort {
    String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl);
    void processRefund(String stripePaymentIntentId, BigDecimal amount);
    String createStripeAccountLink(UUID providerId, String returnUrl, String refreshUrl);

    /**
     * @return true only if Stripe reports the connected account able to receive payouts
     *         (onboarding complete). Used to gate {@code payoutsEnabled}.
     */
    boolean accountPayoutsEnabled(String stripeAccountId);

    /**
     * Transfers the net amount to the provider's connected account.
     *
     * @param sourceChargeId  the Stripe charge id to transfer from (its currency must match
     *                        {@code currency}); may be null if unknown.
     * @param currency        the charge currency (must be the transaction currency, not hardcoded).
     * @return the created Stripe transfer id, so it can be reversed if the payment is refunded.
     */
    String transferToProvider(String sourceChargeId, String stripeAccountId, BigDecimal amount, String currency);

    /** Reverses (part of) a previously created transfer, e.g. when refunding a paid transaction. */
    void reverseProviderTransfer(String stripeTransferId, BigDecimal amount);
}
