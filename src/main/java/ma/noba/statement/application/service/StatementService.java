package ma.noba.statement.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;
import ma.noba.loyalty.infra.JpaLoyaltyPointsTransactionRepository;
import ma.noba.packagegift.domain.model.GiftCard;
import ma.noba.packagegift.domain.port.GiftCardRepository;
import ma.noba.payment.domain.model.PaymentStatus;
import ma.noba.payment.domain.model.PaymentTransaction;
import ma.noba.payment.domain.port.PaymentTransactionRepository;
import ma.noba.referral.domain.model.Referral;
import ma.noba.referral.domain.port.ReferralRepository;
import ma.noba.statement.application.dto.StatementEntry;
import ma.noba.statement.application.dto.StatementResponse;
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
 */
@Service
@RequiredArgsConstructor
public class StatementService {

    private final PaymentTransactionRepository paymentRepository;
    private final GiftCardRepository giftCardRepository;
    private final JpaLoyaltyPointsTransactionRepository loyaltyTxnRepository;
    private final ReferralRepository referralRepository;

    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID userId) {
        List<StatementEntry> entries = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        int totalPoints = 0;

        for (PaymentTransaction tx : paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            entries.add(new StatementEntry(
                    tx.getCreatedAt(), "PAYMENT", "Payment (" + tx.getStatus() + ")",
                    tx.getAmount() != null ? tx.getAmount().negate() : null, tx.getCurrency(),
                    null, tx.getStatus() != null ? tx.getStatus().name() : null,
                    tx.getId() != null ? tx.getId().toString() : null));
            if (tx.getStatus() == PaymentStatus.SUCCEEDED && tx.getAmount() != null) {
                totalSpent = totalSpent.add(tx.getAmount());
            }
        }

        for (GiftCard gc : giftCardRepository.findByBuyerId(userId)) {
            entries.add(new StatementEntry(
                    gc.getCreatedAt(), "GIFT_CARD_PURCHASED",
                    gc.isRedeemed() ? "Gift card (redeemed)" : "Gift card purchased",
                    gc.getAmount() != null ? gc.getAmount().negate() : null, null,
                    null, gc.isRedeemed() ? "REDEEMED" : "ACTIVE",
                    gc.getId() != null ? gc.getId().toString() : null));
        }

        for (LoyaltyPointsTransaction t : loyaltyTxnRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            entries.add(new StatementEntry(
                    t.getCreatedAt(), t.getPoints() >= 0 ? "LOYALTY_EARNED" : "LOYALTY_REDEEMED",
                    t.getReason(), null, null, t.getPoints(), null,
                    t.getReferenceId() != null ? t.getReferenceId().toString() : null));
            totalPoints += t.getPoints();
        }

        for (Referral r : referralRepository.findByReferrerId(userId)) {
            entries.add(new StatementEntry(
                    r.getCreatedAt(), "REFERRAL", "Referral " + r.getStatus(),
                    null, null, null, r.getStatus() != null ? r.getStatus().name() : null,
                    r.getId() != null ? r.getId().toString() : null));
        }

        entries.sort(Comparator.comparing(StatementEntry::at, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return new StatementResponse(userId.toString(), totalSpent, totalPoints, entries);
    }
}
