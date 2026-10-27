package ma.noba.payment.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.application.dto.*;
import ma.noba.payment.domain.model.*;
import ma.noba.payment.domain.port.*;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ValidationException;
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
    private final ProviderServicePricePort providerServicePricePort;

    @Transactional
    public CheckoutResponse createCheckout(UUID userId, CreateCheckoutRequest request) {
        UUID providerId = UUID.fromString(request.getProviderId());
        UUID serviceId = request.getServiceId() != null ? UUID.fromString(request.getServiceId()) : null;

        // Server-authoritative pricing: when a service is purchased, the amount is taken from the
        // provider's service definition and the client-supplied amount is ignored. Only ad-hoc
        // payments without a service (e.g. gift cards) may specify an amount.
        BigDecimal amount;
        if (serviceId != null) {
            amount = providerServicePricePort.findActiveServicePrice(providerId, serviceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROVIDER_SERVICE_NOT_FOUND,
                    "Service not found for provider"));
        } else {
            if (request.getAmount() == null) {
                throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "amount is required when no service is specified");
            }
            amount = request.getAmount();
        }

        PaymentTransaction transaction = new PaymentTransaction(
            userId,
            providerId,
            serviceId,
            request.getCurrency(),
            amount
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
        // Pessimistic lock + terminal-status guard make this idempotent under concurrent or
        // replayed Stripe deliveries (previously a replay after a refund triggered a second transfer).
        PaymentTransaction transaction = transactionRepository
            .findByStripePaymentIntentIdForUpdate(stripePaymentIntentId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found for PI: " + stripePaymentIntentId));

        if (isTerminal(transaction.getStatus())) {
            log.warn("Ignoring duplicate payment success webhook: PI={}, status={}", stripePaymentIntentId, transaction.getStatus());
            return;
        }

        BigDecimal fee = transaction.getAmount().multiply(NOBA_FEE_PERCENTAGE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        transaction.markSucceeded(stripePaymentIntentId, stripeChargeId, fee);
        transactionRepository.save(transaction);

        ProviderPayoutSettings settings = payoutSettingsRepository.findByProviderId(transaction.getProviderId()).orElse(null);
        if (settings != null && settings.isPayoutsEnabled()) {
            BigDecimal netAmount = transaction.getNetAmount();
            // Transfer from the charge id (not the PaymentIntent) and in the transaction's own
            // currency; record the returned transfer id so a later refund can reverse it.
            String transferId = stripePaymentPort.transferToProvider(
                transaction.getStripeChargeId(), settings.getStripeAccountId(), netAmount, transaction.getCurrency());
            transaction.markTransferred(transferId);
            transactionRepository.save(transaction);

            Payout payout = new Payout(transaction.getProviderId(), netAmount, transaction.getCurrency());
            payout.markProcessing(transferId);
            payoutRepository.save(payout);
        }
    }

    @Transactional
    public void handlePaymentFailure(String stripePaymentIntentId, String failureMessage) {
        PaymentTransaction transaction = transactionRepository
            .findByStripePaymentIntentIdForUpdate(stripePaymentIntentId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found for PI: " + stripePaymentIntentId));

        // Never downgrade an already-settled transaction from a late/replayed failure event.
        if (transaction.getStatus() == PaymentStatus.FAILED || transaction.getStatus() == PaymentStatus.SUCCEEDED
                || transaction.getStatus() == PaymentStatus.REFUNDED
                || transaction.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            log.warn("Ignoring payment failure webhook for settled transaction: PI={}, status={}",
                    stripePaymentIntentId, transaction.getStatus());
            return;
        }

        transaction.markFailed(failureMessage);
        transactionRepository.save(transaction);
    }

    private static boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.PARTIALLY_REFUNDED;
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

        // Only enable payouts if Stripe confirms the connected account can receive them.
        boolean verified = stripePaymentPort.accountPayoutsEnabled(stripeAccountId);
        settings.updateStripeAccount(stripeAccountId, verified);
        payoutSettingsRepository.save(settings);
    }

    public Page<Payout> getProviderPayouts(UUID providerId, int page, int size) {
        return payoutRepository.findByProviderId(providerId, PageRequest.of(page, size));
    }

    @Transactional
    public void processRefund(UUID transactionId) {
        // Row lock + status guard prevent two concurrent refunds from both passing the check.
        PaymentTransaction transaction = transactionRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        if (transaction.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Can only refund succeeded transactions");
        }

        // Claw back the provider's net transfer first, so a refund cannot leave the platform having
        // both returned the customer's money and paid the provider out.
        if (transaction.getStripeTransferId() != null && transaction.getNetAmount() != null) {
            stripePaymentPort.reverseProviderTransfer(transaction.getStripeTransferId(), transaction.getNetAmount());
        }

        stripePaymentPort.processRefund(transaction.getStripePaymentIntentId(), transaction.getAmount());
        transaction.markRefunded();
        transactionRepository.save(transaction);
    }
}
