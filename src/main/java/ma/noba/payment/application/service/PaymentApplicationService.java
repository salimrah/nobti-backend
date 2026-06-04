package ma.noba.payment.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.application.dto.*;
import ma.noba.payment.domain.model.*;
import ma.noba.payment.domain.port.*;
import org.springframework.data.domain.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private static final BigDecimal NOBA_FEE_PERCENTAGE = new BigDecimal("0.05");

    private final PaymentTransactionRepository transactionRepository;
    private final PayoutRepository payoutRepository;
    private final ProviderPayoutSettingsRepository payoutSettingsRepository;
    private final StripePaymentPort stripePaymentPort;

    @Transactional
    public CheckoutResponse createCheckout(UUID userId, CreateCheckoutRequest request) {
        PaymentTransaction transaction = new PaymentTransaction(
            userId,
            UUID.fromString(request.getProviderId()),
            request.getServiceId() != null ? UUID.fromString(request.getServiceId()) : null,
            request.getCurrency(),
            request.getAmount()
        );
        transaction = transactionRepository.save(transaction);

        String clientSecret = stripePaymentPort.createPaymentIntent(
            transaction,
            null,
            request.getReturnUrl()
        );

        return new CheckoutResponse(
            transaction.getStripePaymentIntentId(),
            clientSecret,
            transaction.getId().toString(),
            transaction.getStatus().name()
        );
    }

    @Transactional
    public void handlePaymentSuccess(String stripePaymentIntentId, String stripeChargeId) {
        PaymentTransaction transaction = transactionRepository
            .findByStripePaymentIntentId(stripePaymentIntentId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found for PI: " + stripePaymentIntentId));

        if (transaction.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Duplicate payment success webhook: PI={}, already SUCCEEDED", stripePaymentIntentId);
            return;
        }

        BigDecimal fee = transaction.getAmount().multiply(NOBA_FEE_PERCENTAGE);
        transaction.markSucceeded(stripePaymentIntentId, stripeChargeId, fee);
        transactionRepository.save(transaction);

        ProviderPayoutSettings settings = payoutSettingsRepository.findByProviderId(transaction.getProviderId()).orElse(null);
        if (settings != null && settings.isPayoutsEnabled()) {
            BigDecimal netAmount = transaction.getNetAmount();
            stripePaymentPort.transferToProvider(stripePaymentIntentId, settings.getStripeAccountId(), netAmount);

            Payout payout = new Payout(transaction.getProviderId(), netAmount, transaction.getCurrency());
            payout.markProcessing(stripePaymentIntentId);
            payoutRepository.save(payout);
        }
    }

    @Transactional
    public void handlePaymentFailure(String stripePaymentIntentId, String failureMessage) {
        PaymentTransaction transaction = transactionRepository
            .findByStripePaymentIntentId(stripePaymentIntentId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found for PI: " + stripePaymentIntentId));

        if (transaction.getStatus() == PaymentStatus.FAILED) {
            log.warn("Duplicate payment failure webhook: PI={}, already FAILED", stripePaymentIntentId);
            return;
        }

        transaction.markFailed(failureMessage);
        transactionRepository.save(transaction);
    }

    public PaymentTransaction getTransaction(UUID transactionId) {
        return transactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
    }

    public Page<PaymentTransaction> getUserTransactions(UUID userId, int page, int size) {
        return transactionRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    public Page<PaymentTransaction> getProviderTransactions(UUID providerId, int page, int size) {
        return transactionRepository.findByProviderId(providerId, PageRequest.of(page, size));
    }

    @Transactional
    public PayoutSettingsResponse getOrCreatePayoutSettings(UUID providerId) {
        ProviderPayoutSettings settings = payoutSettingsRepository
            .findByProviderId(providerId)
            .orElseGet(() -> {
                ProviderPayoutSettings newSettings = new ProviderPayoutSettings(providerId);
                return payoutSettingsRepository.save(newSettings);
            });

        String accountLink = null;
        if (!settings.isPayoutsEnabled()) {
            accountLink = stripePaymentPort.createStripeAccountLink(
                providerId,
                "https://noba.ma/provider/payouts",
                "https://noba.ma/provider/payouts"
            );
        }

        return new PayoutSettingsResponse(
            providerId.toString(),
            settings.isPayoutsEnabled(),
            settings.getStripeAccountId() != null,
            accountLink
        );
    }

    @Transactional
    public void updateStripeAccount(UUID providerId, String stripeAccountId) {
        ProviderPayoutSettings settings = payoutSettingsRepository
            .findByProviderId(providerId)
            .orElseThrow(() -> new IllegalArgumentException("Payout settings not found for provider: " + providerId));

        settings.updateStripeAccount(stripeAccountId);
        payoutSettingsRepository.save(settings);
    }

    public Page<Payout> getProviderPayouts(UUID providerId, int page, int size) {
        return payoutRepository.findByProviderId(providerId, PageRequest.of(page, size));
    }

    @Transactional
    public void processRefund(UUID transactionId) {
        PaymentTransaction transaction = getTransaction(transactionId);
        if (transaction.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Can only refund succeeded transactions");
        }

        stripePaymentPort.processRefund(transaction.getStripePaymentIntentId(), transaction.getAmount());
        transaction.markRefunded();
        transactionRepository.save(transaction);
    }
}
