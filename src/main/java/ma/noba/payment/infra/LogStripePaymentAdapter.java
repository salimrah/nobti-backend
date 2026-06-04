package ma.noba.payment.infra;

import lombok.extern.slf4j.Slf4j;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.StripePaymentPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnMissingBean(StripePaymentPort.class)
public class LogStripePaymentAdapter implements StripePaymentPort {

    @Override
    public String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl) {
        log.warn("Stripe not configured — would create PaymentIntent for transaction {} amount {}",
            transaction.getId(), transaction.getAmount());
        transaction.setStripePaymentIntentId("pi_mock_" + transaction.getId());
        return "pi_mock_secret_" + transaction.getId();
    }

    @Override
    public void processRefund(String stripePaymentIntentId, BigDecimal amount) {
        log.warn("Stripe not configured — would refund PI: {} amount {}", stripePaymentIntentId, amount);
    }

    @Override
    public String createStripeAccountLink(UUID providerId, String returnUrl, String refreshUrl) {
        log.warn("Stripe not configured — would create account link for provider: {}", providerId);
        return refreshUrl;
    }

    @Override
    public void transferToProvider(String stripePaymentIntentId, String stripeAccountId, BigDecimal amount) {
        log.warn("Stripe not configured — would transfer {} to {} for PI: {}", amount, stripeAccountId, stripePaymentIntentId);
    }
}
