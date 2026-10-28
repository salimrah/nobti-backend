package ma.noba.payment;

import ma.noba.payment.application.dto.CreateCheckoutRequest;
import ma.noba.payment.application.service.PaymentApplicationService;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.model.Payout;
import ma.noba.payment.domain.model.ProviderPayoutSettings;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.payment.domain.port.PayoutRepository;
import ma.noba.payment.domain.port.ProviderPayoutSettingsRepository;
import ma.noba.payment.domain.port.ProviderServicePricePort;
import ma.noba.payment.domain.port.PaymentGatewayPort;
import ma.noba.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    @Mock PaymentTransactionRepository transactionRepository;
    @Mock PayoutRepository payoutRepository;
    @Mock ProviderPayoutSettingsRepository payoutSettingsRepository;
    @Mock PaymentGatewayPort paymentGateway;
    @Mock ProviderServicePricePort providerServicePricePort;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(transactionRepository, payoutRepository, payoutSettingsRepository,
                paymentGateway, providerServicePricePort);
    }

    private void stubCheckoutPersistence() {
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(paymentGateway.createPaymentIntent(any(), any(), any())).thenReturn("secret");
    }

    private PaymentTransaction captureSavedTransaction() {
        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private PaymentTransaction pendingTransaction() {
        return new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(), null, "USD", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("A replayed success event for an already-succeeded transaction is ignored")
    void alreadySucceededReplayIsIgnored() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        when(transactionRepository.findByStripePaymentIntentIdForUpdate("pi_1")).thenReturn(Optional.of(tx));

        service.handlePaymentSuccess("pi_1", "ch_1");

        verify(transactionRepository, never()).save(any());
        verify(paymentGateway, never()).transferToProvider(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("A success event replayed after a refund does not trigger a second transfer")
    void refundedTransactionReplayIsIgnored() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        tx.markRefunded();
        when(transactionRepository.findByStripePaymentIntentIdForUpdate("pi_1")).thenReturn(Optional.of(tx));

        service.handlePaymentSuccess("pi_1", "ch_2");

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(transactionRepository, never()).save(any());
        verify(paymentGateway, never()).transferToProvider(anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("A pending transaction is settled once and the net amount is transferred")
    void pendingTransactionIsSettledAndTransferred() {
        PaymentTransaction tx = pendingTransaction();
        ProviderPayoutSettings settings = new ProviderPayoutSettings(tx.getProviderId());
        settings.updateStripeAccount("acct_1", true);
        when(transactionRepository.findByStripePaymentIntentIdForUpdate("pi_1")).thenReturn(Optional.of(tx));
        when(payoutSettingsRepository.findByProviderId(tx.getProviderId())).thenReturn(Optional.of(settings));
        when(transactionRepository.save(any())).thenReturn(tx);
        when(paymentGateway.transferToProvider("ch_1", "acct_1", new BigDecimal("95.00"), "USD"))
                .thenReturn("tr_1");

        service.handlePaymentSuccess("pi_1", "ch_1");

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(tx.getFee()).isEqualByComparingTo("5.00");
        assertThat(tx.getNetAmount()).isEqualByComparingTo("95.00");
        // Transfer uses the charge id + transaction currency (not the PaymentIntent id / hardcoded usd).
        verify(paymentGateway).transferToProvider("ch_1", "acct_1", new BigDecimal("95.00"), "USD");
        assertThat(tx.getStripeTransferId()).isEqualTo("tr_1");

        ArgumentCaptor<Payout> payoutCaptor = ArgumentCaptor.forClass(Payout.class);
        verify(payoutRepository).save(payoutCaptor.capture());
        assertThat(payoutCaptor.getValue().getStripeTransferId()).isEqualTo("tr_1");
    }

    @Test
    @DisplayName("A payment failure marks the transaction FAILED with the reason")
    void paymentFailureMarksTransactionFailed() {
        PaymentTransaction tx = pendingTransaction();
        when(transactionRepository.findByStripePaymentIntentIdForUpdate("pi_1")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.handlePaymentFailure("pi_1", "card_declined");

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(tx.getFailureMessage()).isEqualTo("card_declined");
    }

    @Test
    @DisplayName("Refund without a recorded transfer does not attempt a transfer reversal")
    void refundWithoutRecordedTransferDoesNotReverse() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.processRefund(id);

        verify(paymentGateway, never()).reverseProviderTransfer(anyString(), any());
        verify(paymentGateway).processRefund("pi_1", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Refund reverses the provider transfer and marks the transaction refunded")
    void refundReversesTransferAndMarksRefunded() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        tx.markTransferred("tr_1");
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.processRefund(id);

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentGateway).reverseProviderTransfer("tr_1", new BigDecimal("95.00"));
        verify(paymentGateway).processRefund("pi_1", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Refund is rejected for a transaction that is not SUCCEEDED")
    void refundRejectsNonSucceeded() {
        PaymentTransaction tx = pendingTransaction();
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.processRefund(id)).isInstanceOf(IllegalStateException.class);

        verify(paymentGateway, never()).processRefund(anyString(), any());
    }

    @Test
    @DisplayName("A transaction cannot be refunded twice")
    void refundCannotBeAppliedTwice() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdForUpdate(id)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenReturn(tx);

        service.processRefund(id);

        assertThatThrownBy(() -> service.processRefund(id)).isInstanceOf(IllegalStateException.class);
        verify(paymentGateway).processRefund("pi_1", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("A late failure event does not downgrade a succeeded transaction")
    void failureDoesNotDowngradeSucceeded() {
        PaymentTransaction tx = pendingTransaction();
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        when(transactionRepository.findByStripePaymentIntentIdForUpdate("pi_1")).thenReturn(Optional.of(tx));

        service.handlePaymentFailure("pi_1", "declined");

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Linking a Stripe account does not enable payouts unless Stripe confirms it")
    void updateStripeAccountRequiresVerification() {
        UUID providerId = UUID.randomUUID();
        ProviderPayoutSettings settings = new ProviderPayoutSettings(providerId);
        when(payoutSettingsRepository.findByProviderId(providerId)).thenReturn(Optional.of(settings));
        when(paymentGateway.accountPayoutsEnabled("acct_unverified")).thenReturn(false);
        when(payoutSettingsRepository.save(any())).thenReturn(settings);

        service.updateStripeAccount(providerId, "acct_unverified");

        assertThat(settings.isPayoutsEnabled()).isFalse();
        assertThat(settings.getStripeAccountId()).isEqualTo("acct_unverified");
    }

    @Test
    @DisplayName("Checkout derives the amount from the service price, ignoring the client amount")
    void checkoutUsesAuthoritativeServicePrice() {
        UUID providerId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        CreateCheckoutRequest request = new CreateCheckoutRequest(
                providerId.toString(), serviceId.toString(), "USD", new BigDecimal("1.00"), null);
        stubCheckoutPersistence();
        when(providerServicePricePort.findActiveServicePrice(providerId, serviceId))
                .thenReturn(Optional.of(new BigDecimal("500.00")));

        service.createCheckout(UUID.randomUUID(), request);

        assertThat(captureSavedTransaction().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Checkout without a service keeps the client-supplied amount (e.g. gift cards)")
    void checkoutWithoutServiceUsesClientAmount() {
        UUID providerId = UUID.randomUUID();
        CreateCheckoutRequest request = new CreateCheckoutRequest(
                providerId.toString(), null, "USD", new BigDecimal("100.00"), null);
        stubCheckoutPersistence();

        service.createCheckout(UUID.randomUUID(), request);

        assertThat(captureSavedTransaction().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Checkout for an unknown/unowned service is rejected")
    void checkoutRejectsUnknownService() {
        UUID providerId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        CreateCheckoutRequest request = new CreateCheckoutRequest(
                providerId.toString(), serviceId.toString(), "USD", new BigDecimal("1.00"), null);
        when(providerServicePricePort.findActiveServicePrice(providerId, serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCheckout(UUID.randomUUID(), request))
                .isInstanceOf(NotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
