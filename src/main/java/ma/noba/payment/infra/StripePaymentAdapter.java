package ma.noba.payment.infra;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.extern.slf4j.Slf4j;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.StripePaymentPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.stripe.secret-key:}')")
public class StripePaymentAdapter implements StripePaymentPort {

    @Override
    public String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("transaction_id", transaction.getId().toString());
            metadata.put("provider_id", transaction.getProviderId().toString());
            metadata.put("user_id", transaction.getUserId().toString());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(transaction.getAmount().multiply(new BigDecimal("100")).longValue())
                .setCurrency(transaction.getCurrency().toLowerCase())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            transaction.setStripePaymentIntentId(paymentIntent.getId());

            return paymentIntent.getClientSecret();
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent", e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Override
    public void processRefund(String stripePaymentIntentId, BigDecimal amount) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(stripePaymentIntentId)
                .setAmount(amount.multiply(new BigDecimal("100")).longValue())
                .build();

            Refund.create(params);
            log.info("Refund processed for PI: {}", stripePaymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to process refund for PI: {}", stripePaymentIntentId, e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage(), e);
        }
    }

    @Override
    public String createStripeAccountLink(UUID providerId, String returnUrl, String refreshUrl) {
        try {
            AccountCreateParams accountParams = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .putMetadata("provider_id", providerId.toString())
                .setBusinessType(AccountCreateParams.BusinessType.INDIVIDUAL)
                .build();

            Account account = Account.create(accountParams);

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(account.getId())
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

            AccountLink accountLink = AccountLink.create(linkParams);

            return accountLink.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create Stripe account link for provider: {}", providerId, e);
            throw new RuntimeException("Failed to create account link: " + e.getMessage(), e);
        }
    }

    @Override
    public void transferToProvider(String stripePaymentIntentId, String stripeAccountId, BigDecimal amount) {
        try {
            Long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
            if (amountInCents <= 0) {
                log.warn("Skipping zero/negative transfer for PI: {}", stripePaymentIntentId);
                return;
            }

            TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setDestination(stripeAccountId)
                .setSourceTransaction(stripePaymentIntentId)
                .build();

            Transfer.create(params);
            log.info("Transfer of {} to {} for PI: {}", amount, stripeAccountId, stripePaymentIntentId);
        } catch (StripeException e) {
            log.error("Failed to transfer to provider account: {}", stripeAccountId, e);
            throw new RuntimeException("Failed to transfer to provider: " + e.getMessage(), e);
        }
    }
}
