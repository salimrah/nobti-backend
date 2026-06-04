package ma.noba.payment.domain.port;

import ma.noba.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.UUID;

public interface StripePaymentPort {
    String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl);
    void processRefund(String stripePaymentIntentId, BigDecimal amount);
    String createStripeAccountLink(UUID providerId, String returnUrl, String refreshUrl);
    void transferToProvider(String stripePaymentIntentId, String stripeAccountId, BigDecimal amount);
}
