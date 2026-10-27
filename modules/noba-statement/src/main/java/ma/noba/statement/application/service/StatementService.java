package ma.noba.statement.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.statement.application.dto.StatementEntry;
import ma.noba.statement.application.dto.StatementResponse;
import ma.noba.statement.domain.port.StatementGiftCardSource;
import ma.noba.statement.domain.port.StatementLoyaltySource;
import ma.noba.statement.domain.port.StatementPaymentSource;
import ma.noba.statement.domain.port.StatementReferralSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds a single chronological financial statement for a user by aggregating payments, gift cards,
 * loyalty points and referrals — so users (and support) can see all movements in one place instead
 * of piecing them together from separate endpoints.
 *
 * <p>Each source is a port owned by this module and implemented in the composition root, so the
 * statement never depends on another module's persistence entities.
 */
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementPaymentSource paymentSource;
    private final StatementGiftCardSource giftCardSource;
    private final StatementLoyaltySource loyaltySource;
    private final StatementReferralSource referralSource;

    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID userId) {
        List<StatementEntry> entries = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        int totalPoints = 0;

        for (StatementPaymentSource.PaymentLine payment : paymentSource.findPayments(userId)) {
            entries.add(new StatementEntry(
                    payment.createdAt(), "PAYMENT", "Payment (" + payment.status() + ")",
                    payment.amount() != null ? payment.amount().negate() : null, payment.currency(),
                    null, payment.status(),
                    payment.id() != null ? payment.id().toString() : null));
            if (payment.succeeded() && payment.amount() != null) {
                totalSpent = totalSpent.add(payment.amount());
            }
        }

        for (StatementGiftCardSource.GiftCardLine card : giftCardSource.findGiftCards(userId)) {
            entries.add(new StatementEntry(
                    card.createdAt(), "GIFT_CARD_PURCHASED",
                    card.redeemed() ? "Gift card (redeemed)" : "Gift card purchased",
                    card.amount() != null ? card.amount().negate() : null, null,
                    null, card.redeemed() ? "REDEEMED" : "ACTIVE",
                    card.id() != null ? card.id().toString() : null));
        }

        for (StatementLoyaltySource.LoyaltyLine movement : loyaltySource.findLoyaltyMovements(userId)) {
            entries.add(new StatementEntry(
                    movement.createdAt(),
                    movement.points() >= 0 ? "LOYALTY_EARNED" : "LOYALTY_REDEEMED",
                    movement.reason(), null, null, movement.points(), null,
                    movement.referenceId() != null ? movement.referenceId().toString() : null));
            totalPoints += movement.points();
        }

        for (StatementReferralSource.ReferralLine referral : referralSource.findReferrals(userId)) {
            entries.add(new StatementEntry(
                    referral.createdAt(), "REFERRAL", "Referral " + referral.status(),
                    null, null, null, referral.status(),
                    referral.id() != null ? referral.id().toString() : null));
        }

        entries.sort(Comparator.comparing(StatementEntry::at, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return new StatementResponse(userId.toString(), totalSpent, totalPoints, entries);
    }
}
