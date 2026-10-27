package ma.noba.packagegift;

import ma.noba.packagegift.application.dto.PurchaseGiftCardRequest;
import ma.noba.packagegift.application.service.GiftCardApplicationService;
import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GiftCardApplicationServiceTest {

    @Mock GiftCardRepository giftCardRepository;
    @Mock PaymentTransactionRepository paymentTransactionRepository;

    private GiftCardApplicationService service;

    private final UUID buyerId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GiftCardApplicationService(giftCardRepository, paymentTransactionRepository);
    }

    private PaymentTransaction transaction(UUID owner, BigDecimal amount, boolean succeeded) {
        PaymentTransaction tx = new PaymentTransaction(owner, providerId, null, "USD", amount);
        if (succeeded) {
            tx.markSucceeded("pi_1", "ch_1", new BigDecimal("0.00"));
        }
        return tx;
    }

    private PurchaseGiftCardRequest request(BigDecimal amount, UUID txId) {
        PurchaseGiftCardRequest req = new PurchaseGiftCardRequest();
        req.setProviderId(providerId);
        req.setAmount(amount);
        req.setPaymentTransactionId(txId);
        req.setRecipientName("Friend");
        req.setMessage("Enjoy");
        return req;
    }

    @Test
    @DisplayName("A gift card cannot be issued without a SUCCEEDED payment")
    void purchaseWithoutSuccessfulPaymentIsRejected() {
        UUID txId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(txId))
                .thenReturn(Optional.of(transaction(buyerId, new BigDecimal("100.00"), false)));

        assertThatThrownBy(() -> service.purchaseGiftCard(buyerId, request(new BigDecimal("100.00"), txId)))
                .isInstanceOf(ValidationException.class);

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("A payment belonging to another user cannot fund a gift card")
    void purchaseWithForeignPaymentIsDenied() {
        UUID txId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(txId))
                .thenReturn(Optional.of(transaction(otherUserId, new BigDecimal("100.00"), true)));

        assertThatThrownBy(() -> service.purchaseGiftCard(buyerId, request(new BigDecimal("100.00"), txId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("A payment for a different provider cannot fund the gift card")
    void purchaseWithProviderMismatchIsRejected() {
        UUID txId = UUID.randomUUID();
        PaymentTransaction tx = new PaymentTransaction(buyerId, UUID.randomUUID(), null, "USD", new BigDecimal("100.00"));
        tx.markSucceeded("pi_1", "ch_1", new BigDecimal("0.00"));
        when(paymentTransactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> service.purchaseGiftCard(buyerId, request(new BigDecimal("100.00"), txId)))
                .isInstanceOf(ValidationException.class);

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("The gift card amount must match the amount actually paid")
    void purchaseWithAmountMismatchIsRejected() {
        UUID txId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(txId))
                .thenReturn(Optional.of(transaction(buyerId, new BigDecimal("1.00"), true)));

        assertThatThrownBy(() -> service.purchaseGiftCard(buyerId, request(new BigDecimal("10000.00"), txId)))
                .isInstanceOf(ValidationException.class);

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("A single payment cannot be reused to mint multiple gift cards")
    void purchaseWithReusedPaymentIsRejected() {
        UUID txId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(txId))
                .thenReturn(Optional.of(transaction(buyerId, new BigDecimal("100.00"), true)));
        when(giftCardRepository.existsByPaymentTransactionId(any())).thenReturn(true);

        assertThatThrownBy(() -> service.purchaseGiftCard(buyerId, request(new BigDecimal("100.00"), txId)))
                .isInstanceOf(ValidationException.class);

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("A matching SUCCEEDED payment issues a linked gift card")
    void purchaseWithSucceededPaymentIssuesCard() {
        UUID txId = UUID.randomUUID();
        when(paymentTransactionRepository.findById(txId))
                .thenReturn(Optional.of(transaction(buyerId, new BigDecimal("100.00"), true)));
        lenient().when(giftCardRepository.existsByPaymentTransactionId(any())).thenReturn(false);
        when(giftCardRepository.save(any())).thenAnswer(invocation -> {
            GiftCard saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        var response = service.purchaseGiftCard(buyerId, request(new BigDecimal("100.00"), txId));

        assertThat(response.getCode()).startsWith("NOBA-");
        assertThat(response.getBuyerId()).isEqualTo(buyerId.toString());
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
    }
}
