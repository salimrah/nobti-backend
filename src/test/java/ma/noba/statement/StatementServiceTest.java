package ma.noba.statement;

import ma.noba.statement.application.dto.StatementEntry;
import ma.noba.statement.application.service.StatementService;
import ma.noba.statement.domain.port.StatementGiftCardSource;
import ma.noba.statement.domain.port.StatementLoyaltySource;
import ma.noba.statement.domain.port.StatementPaymentSource;
import ma.noba.statement.domain.port.StatementReferralSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock StatementPaymentSource paymentSource;
    @Mock StatementGiftCardSource giftCardSource;
    @Mock StatementLoyaltySource loyaltySource;
    @Mock StatementReferralSource referralSource;

    private StatementService service;

    private final UUID userId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-27T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new StatementService(paymentSource, giftCardSource, loyaltySource, referralSource);
    }

    @Test
    @DisplayName("Statement aggregates payments, gift cards and loyalty with totals")
    void aggregatesSources() {
        var paid = new StatementPaymentSource.PaymentLine(
                now, true, "SUCCEEDED", new BigDecimal("100.00"), "MAD", UUID.randomUUID());
        var failed = new StatementPaymentSource.PaymentLine(
                now.plusSeconds(60), false, "FAILED", new BigDecimal("50.00"), "MAD", UUID.randomUUID());
        var card = new StatementGiftCardSource.GiftCardLine(
                now.plusSeconds(120), new BigDecimal("30.00"), false, UUID.randomUUID());
        var earned = new StatementLoyaltySource.LoyaltyLine(now.plusSeconds(180), 10, "SERVICE_COMPLETED", null);
        var redeemed = new StatementLoyaltySource.LoyaltyLine(now.plusSeconds(240), -5, "REDEEMED:Free cut", null);

        when(paymentSource.findPayments(userId)).thenReturn(List.of(paid, failed));
        when(giftCardSource.findGiftCards(userId)).thenReturn(List.of(card));
        when(loyaltySource.findLoyaltyMovements(userId)).thenReturn(List.of(earned, redeemed));
        when(referralSource.findReferrals(userId)).thenReturn(List.of());

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
        when(paymentSource.findPayments(userId)).thenReturn(List.of());
        when(giftCardSource.findGiftCards(userId)).thenReturn(List.of());
        when(loyaltySource.findLoyaltyMovements(userId)).thenReturn(List.of());
        when(referralSource.findReferrals(userId)).thenReturn(List.of());

        var statement = service.getStatement(userId);

        assertThat(statement.entries()).isEmpty();
        assertThat(statement.totalSpent()).isEqualByComparingTo("0");
        assertThat(statement.totalPoints()).isZero();
    }
}
