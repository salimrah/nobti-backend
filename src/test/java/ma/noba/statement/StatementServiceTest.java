package ma.noba.statement;

import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;
import ma.noba.loyalty.infra.JpaLoyaltyPointsTransactionRepository;
import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.referral.domain.port.ReferralRepository;
import ma.noba.statement.application.dto.StatementEntry;
import ma.noba.statement.application.service.StatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock PaymentTransactionRepository paymentRepository;
    @Mock GiftCardRepository giftCardRepository;
    @Mock JpaLoyaltyPointsTransactionRepository loyaltyTxnRepository;
    @Mock ReferralRepository referralRepository;

    private StatementService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StatementService(paymentRepository, giftCardRepository, loyaltyTxnRepository, referralRepository);
    }

    @Test
    @DisplayName("Statement aggregates payments, gift cards and loyalty with totals")
    void aggregatesSources() {
        PaymentTransaction paid = new PaymentTransaction(userId, providerId, null, "USD", new BigDecimal("100.00"));
        paid.markSucceeded("pi_1", "ch_1", new BigDecimal("5.00"));
        PaymentTransaction failed = new PaymentTransaction(userId, providerId, null, "USD", new BigDecimal("50.00"));
        failed.markFailed("declined");

        GiftCard giftCard = new GiftCard(providerId, userId, new BigDecimal("30.00"), "NOBA-TEST-0001");
        LoyaltyPointsTransaction earned = new LoyaltyPointsTransaction(userId, providerId, 10, "SERVICE_COMPLETED", null);
        LoyaltyPointsTransaction redeemed = new LoyaltyPointsTransaction(userId, providerId, -5, "REDEEMED:Free cut", null);

        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(paid, failed));
        when(giftCardRepository.findByBuyerId(userId)).thenReturn(List.of(giftCard));
        when(loyaltyTxnRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(earned, redeemed));
        when(referralRepository.findByReferrerId(userId)).thenReturn(List.of());

        var statement = service.getStatement(userId);

        assertThat(statement.totalSpent()).isEqualByComparingTo("100.00");
        assertThat(statement.totalPoints()).isEqualTo(5);
        assertThat(statement.entries()).extracting(StatementEntry::type)
                .contains("PAYMENT", "GIFT_CARD_PURCHASED", "LOYALTY_EARNED", "LOYALTY_REDEEMED");
        // A payment is money leaving the user, so it is stored as a negative amount.
        assertThat(statement.entries().stream()
                .filter(e -> "PAYMENT".equals(e.type()) && "SUCCEEDED".equals(e.status()))
                .findFirst().orElseThrow().amount())
                .isEqualByComparingTo("-100.00");
    }

    @Test
    @DisplayName("Statement is empty (but valid) for a user with no activity")
    void emptyStatement() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(giftCardRepository.findByBuyerId(userId)).thenReturn(List.of());
        when(loyaltyTxnRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(referralRepository.findByReferrerId(userId)).thenReturn(List.of());

        var statement = service.getStatement(userId);

        assertThat(statement.entries()).isEmpty();
        assertThat(statement.totalSpent()).isEqualByComparingTo("0");
        assertThat(statement.totalPoints()).isZero();
    }
}
