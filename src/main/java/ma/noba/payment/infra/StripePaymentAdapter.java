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
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.stripe.secret-key:}')")
public class StripePaymentAdapter implements StripePaymentPort {

    /**
     * Converts a major-unit amount to Stripe minor units, rounding to 2 decimals half-up and
     * rejecting lossy conversions instead of silently truncating.
     */
    static long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    @Override
    public String createPaymentIntent(PaymentTransaction transaction, String customerEmail, String returnUrl) {
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("transaction_id", transaction.getId().toString());
            metadata.put("provider_id", transaction.getProviderId().toString());
            metadata.put("user_id", transaction.getUserId().toString());

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toMinorUnits(transaction.getAmount()))
                .setCurrency(transaction.getCurrency().toLowerCase(Locale.ROOT))
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
                .setAmount(toMinorUnits(amount))
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
    public String transferToProvider(String sourceChargeId, String stripeAccountId, BigDecimal amount, String currency) {
        try {
            long amountInMinorUnits = toMinorUnits(amount);
            if (amountInMinorUnits <= 0) {
                log.warn("Skipping zero/negative transfer to account {}", stripeAccountId);
                return null;
            }

            TransferCreateParams.Builder builder = TransferCreateParams.builder()
                .setAmount(amountInMinorUnits)
                .setCurrency(currency.toLowerCase(Locale.ROOT))
                .setDestination(stripeAccountId);

            // A transfer's source must be a charge (not a PaymentIntent), and it must be in the
            // same currency as the charge.
            if (sourceChargeId != null && !sourceChargeId.isBlank()) {
                builder.setSourceTransaction(sourceChargeId);
            }

            Transfer transfer = Transfer.create(builder.build());
            log.info("Transfer {} of {} {} to {} (charge {})",
                transfer.getId(), amount, currency, stripeAccountId, sourceChargeId);
            return transfer.getId();
        } catch (StripeException e) {
            log.error("Failed to transfer to provider account: {}", stripeAccountId, e);
            throw new RuntimeException("Failed to transfer to provider: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean accountPayoutsEnabled(String stripeAccountId) {
        try {
            Account account = Account.retrieve(stripeAccountId);
            return Boolean.TRUE.equals(account.getPayoutsEnabled());
        } catch (StripeException e) {
            log.error("Failed to verify Stripe account: {}", stripeAccountId, e);
            return false;
        }
    }

    @Override
    public void reverseProviderTransfer(String stripeTransferId, BigDecimal amount) {
        try {
            long amountInMinorUnits = toMinorUnits(amount);
            if (amountInMinorUnits <= 0) {
                log.warn("Skipping zero/negative transfer reversal for transfer {}", stripeTransferId);
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountInMinorUnits);
            TransferReversal reversal = Transfer.retrieve(stripeTransferId).getReversals().create(params);
            log.info("Reversed transfer {} by {} (reversal {})", stripeTransferId, amount, reversal.getId());
        } catch (StripeException e) {
            log.error("Failed to reverse transfer {}", stripeTransferId, e);
            throw new RuntimeException("Failed to reverse transfer: " + e.getMessage(), e);
        }
    }
}
