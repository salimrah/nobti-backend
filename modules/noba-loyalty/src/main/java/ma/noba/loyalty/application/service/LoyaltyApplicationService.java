package ma.noba.loyalty.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.loyalty.application.dto.*;
import ma.noba.loyalty.domain.model.LoyaltyPoints;
import ma.noba.loyalty.domain.model.LoyaltyPointsTransaction;
import ma.noba.loyalty.domain.model.LoyaltyRedemption;
import ma.noba.loyalty.domain.model.LoyaltyReward;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import ma.noba.loyalty.domain.port.LoyaltyRedemptionRepository;
import ma.noba.loyalty.domain.port.LoyaltyRewardRepository;
import ma.noba.loyalty.domain.port.LoyaltyPointsTransactionRepository;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyApplicationService {

    private final LoyaltyPointsRepository pointsRepository;
    private final LoyaltyPointsTransactionRepository txnRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;
    private final ProviderLookupPort providerLookupPort;

    /** Provider display name, falling back to the id only if the provider cannot be resolved. */
    private String providerName(UUID providerId) {
        return providerLookupPort.findProviderName(providerId).orElse(providerId.toString());
    }

    public MyLoyaltyResponse getMyLoyalty(UUID userId) {
        List<LoyaltyPoints> allPoints = pointsRepository.findByUserId(userId);
        List<LoyaltyPointsTransaction> allTxns = txnRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<LoyaltyResponse> providerResponses = allPoints.stream()
            .map(lp -> {
                List<LoyaltyReward> rewards = rewardRepository.findByProviderIdAndIsActive(lp.getProviderId(), true);
                List<PointsHistoryEntry> history = allTxns.stream()
                    .filter(t -> t.getProviderId().equals(lp.getProviderId()))
                    .map(t -> new PointsHistoryEntry(t.getPoints(), t.getReason(), t.getCreatedAt()))
                    .toList();

                return new LoyaltyResponse(
                    lp.getProviderId().toString(), providerName(lp.getProviderId()), lp.getTotalPoints(),
                    lp.getTier(),
                    rewards.stream().map(r -> new LoyaltyRewardResponse(
                        r.getId().toString(), r.getProviderId().toString(),
                        r.getName(), r.getDescription(), r.getPointsRequired(),
                        r.isActive(), r.getCreatedAt()
                    )).toList(),
                    history
                );
            })
            .toList();

        int totalPoints = allPoints.stream().mapToInt(LoyaltyPoints::getTotalPoints).sum();

        List<PointsHistoryEntry> allHistory = allTxns.stream()
            .map(t -> new PointsHistoryEntry(t.getPoints(), t.getReason(), t.getCreatedAt()))
            .toList();

        return new MyLoyaltyResponse(providerResponses, totalPoints, allHistory);
    }

    public LoyaltyResponse getProviderLoyalty(UUID userId, UUID providerId) {
        LoyaltyPoints lp = pointsRepository.findByUserIdAndProviderId(userId, providerId)
            .orElse(new LoyaltyPoints(userId, providerId));

        List<LoyaltyReward> rewards = rewardRepository.findByProviderIdAndIsActive(providerId, true);
        List<LoyaltyPointsTransaction> txns = txnRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(t -> t.getProviderId().equals(providerId))
            .toList();

        List<PointsHistoryEntry> history = txns.stream()
            .map(t -> new PointsHistoryEntry(t.getPoints(), t.getReason(), t.getCreatedAt()))
            .toList();

        return new LoyaltyResponse(
            providerId.toString(), providerName(providerId), lp.getTotalPoints(), lp.getTier(),
            rewards.stream().map(r -> new LoyaltyRewardResponse(
                r.getId().toString(), r.getProviderId().toString(),
                r.getName(), r.getDescription(), r.getPointsRequired(),
                r.isActive(), r.getCreatedAt()
            )).toList(),
            history
        );
    }

    /**
     * Runs in its own transaction: the award is triggered from an {@code AFTER_COMMIT} listener
     * where the caller's transaction is already finished, so joining it would silently fail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void awardPoints(UUID userId, UUID providerId, int points, String reason, UUID referenceId) {
        if (points <= 0) {
            return;
        }
        // Atomic upsert-increment — safe under concurrency and creates the row if absent.
        pointsRepository.addPoints(userId, providerId, points);

        LoyaltyPointsTransaction txn = new LoyaltyPointsTransaction(userId, providerId, points, reason, referenceId);
        txnRepository.saveTransaction(txn);
    }

    @Transactional
    public RedeemRewardResponse redeemReward(UUID userId, UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
            .orElseThrow(() -> new IllegalArgumentException("Reward not found: " + rewardId));

        if (!reward.isActive()) {
            throw new IllegalStateException("Reward is no longer available");
        }

        // Atomic conditional decrement: two concurrent redemptions cannot both pass a balance check.
        int updated = pointsRepository.deductPointsIfSufficient(
            userId, reward.getProviderId(), reward.getPointsRequired());
        if (updated == 0) {
            throw new IllegalStateException("Insufficient loyalty points");
        }

        LoyaltyPointsTransaction txn = new LoyaltyPointsTransaction(
            userId, reward.getProviderId(), -reward.getPointsRequired(),
            "REDEEMED:" + reward.getName(), rewardId
        );
        txnRepository.saveTransaction(txn);

        LoyaltyRedemption redemption = new LoyaltyRedemption(userId, rewardId, reward.getPointsRequired());
        redemption.confirm();
        redemption = redemptionRepository.save(redemption);

        return new RedeemRewardResponse(
            redemption.getId().toString(), reward.getName(),
            reward.getPointsRequired(), redemption.getStatus().name(),
            "Reward redeemed successfully"
        );
    }

    @Transactional
    public LoyaltyRewardResponse createReward(UUID providerId, CreateRewardRequest request) {
        LoyaltyReward reward = new LoyaltyReward(providerId, request.getName(), request.getDescription(), request.getPointsRequired());
        reward = rewardRepository.save(reward);
        return new LoyaltyRewardResponse(
            reward.getId().toString(), reward.getProviderId().toString(),
            reward.getName(), reward.getDescription(), reward.getPointsRequired(),
            reward.isActive(), reward.getCreatedAt()
        );
    }

    @Transactional
    public void deactivateReward(UUID rewardId, UUID providerId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
            .orElseThrow(() -> new IllegalArgumentException("Reward not found: " + rewardId));
        if (!reward.getProviderId().equals(providerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not the provider of this reward");
        }
        reward.deactivate();
        rewardRepository.save(reward);
    }

    public List<LoyaltyRewardResponse> getProviderRewards(UUID providerId) {
        return rewardRepository.findByProviderId(providerId).stream()
            .map(r -> new LoyaltyRewardResponse(
                r.getId().toString(), r.getProviderId().toString(),
                r.getName(), r.getDescription(), r.getPointsRequired(),
                r.isActive(), r.getCreatedAt()
            ))
            .toList();
    }
}
