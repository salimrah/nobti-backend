package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provider-neutral payment gateway contract.
 *
 * <p>Implemented by the Stripe adapter today; a local PSP, cash/bank-transfer flow, or any
 * other provider can be dropped in behind this port without touching the payment domain or the
 * checkout/payout use cases.
 */
public interface PaymentGatewayPort {

    /** Creates a payment intent and returns the client secret/token for the client. */
    String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl);

    /** Refunds (part of) a captured payment. */
    void processRefund(String gatewayPaymentIntentId, BigDecimal amount);

    /** Creates the provider onboarding link for payouts. */
    String createAccountLink(UUID providerId, String returnUrl, String refreshUrl);

    /** @return true only if the provider account is able to receive payouts. */
    boolean accountPayoutsEnabled(String gatewayAccountId);

    /** Transfers the net amount to the provider's account; returns the transfer id. */
    String transferToProvider(String sourceChargeId, String gatewayAccountId, BigDecimal amount, String currency);

    /** Reverses (part of) a previously created transfer. */
    void reverseProviderTransfer(String gatewayTransferId, BigDecimal amount);
}
