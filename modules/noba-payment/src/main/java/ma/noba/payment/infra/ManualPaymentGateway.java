package ma.noba.payment.infra;

import lombok.extern.slf4j.Slf4j;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentGatewayPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Offline gateway for cash / bank-transfer payments (common in Morocco) and for markets where
 * no card PSP is available. Payouts are settled manually by an admin; the payment module still
 * records the transaction, refunds and transfer references.
 *
 * <p>Selected with {@code payment.gateway=manual}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "manual")
public class ManualPaymentGateway implements PaymentGatewayPort {

    private static final String REFERENCE_PREFIX = "manual_";

    @Override
    public String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl) {
        String reference = REFERENCE_PREFIX + transaction.getId();
        transaction.setStripePaymentIntentId(reference);
        log.info("Manual payment recorded for transaction {} (reference {})", transaction.getId(), reference);
        return reference;
    }

    @Override
    public void processRefund(String gatewayPaymentIntentId, BigDecimal amount) {
        log.info("Manual refund recorded for {} amount {}", gatewayPaymentIntentId, amount);
    }

    @Override
    public String createAccountLink(UUID providerId, String returnUrl, String refreshUrl) {
        log.info("Manual payouts for provider {} are managed by an admin; skipping account link", providerId);
        return refreshUrl;
    }

    @Override
    public boolean accountPayoutsEnabled(String gatewayAccountId) {
        // Manual mode has no external onboarding; admins enable payouts explicitly.
        return true;
    }

    @Override
    public String transferToProvider(String sourceChargeId, String gatewayAccountId, BigDecimal amount, String currency) {
        String transferId = "manual_transfer_" + UUID.randomUUID();
        log.info("Manual transfer {} recorded: {} {} to {}", transferId, amount, currency, gatewayAccountId);
        return transferId;
    }

    @Override
    public void reverseProviderTransfer(String gatewayTransferId, BigDecimal amount) {
        log.info("Manual transfer reversal recorded for {} amount {}", gatewayTransferId, amount);
    }
}
