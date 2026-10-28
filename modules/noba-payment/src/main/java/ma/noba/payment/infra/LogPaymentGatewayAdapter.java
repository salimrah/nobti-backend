package ma.noba.payment.infra;

import lombok.extern.slf4j.Slf4j;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentGatewayPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dev/no-credentials adapter: selected when the gateway is "stripe" but no Stripe key is set.
 * It keeps checkout/payout flows working locally without calling a provider.
 */
@Slf4j
@Component
@ConditionalOnExpression("'${payment.gateway:stripe}' == 'stripe' && !T(org.springframework.util.StringUtils).hasText('${app.stripe.secret-key:}')")
public class LogPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl) {
        log.warn("Payment gateway not configured — would create a payment intent for transaction {} amount {}",
            transaction.getId(), transaction.getAmount());
        transaction.setStripePaymentIntentId("pi_mock_" + transaction.getId());
        return "pi_mock_secret_" + transaction.getId();
    }

    @Override
    public void processRefund(String gatewayPaymentIntentId, BigDecimal amount) {
        log.warn("Payment gateway not configured — would refund {} amount {}", gatewayPaymentIntentId, amount);
    }

    @Override
    public String createAccountLink(UUID providerId, String returnUrl, String refreshUrl) {
        log.warn("Payment gateway not configured — would create an account link for provider {}", providerId);
        return refreshUrl;
    }

    @Override
    public String transferToProvider(String sourceChargeId, String gatewayAccountId, BigDecimal amount, String currency) {
        log.warn("Payment gateway not configured — would transfer {} {} to {} from charge {}",
            amount, currency, gatewayAccountId, sourceChargeId);
        return "tr_mock_" + UUID.randomUUID();
    }

    @Override
    public void reverseProviderTransfer(String gatewayTransferId, BigDecimal amount) {
        log.warn("Payment gateway not configured — would reverse transfer {} amount {}", gatewayTransferId, amount);
    }

    @Override
    public boolean accountPayoutsEnabled(String gatewayAccountId) {
        log.warn("Payment gateway not configured — treating account {} as payouts-enabled for local dev", gatewayAccountId);
        return true;
    }
}
