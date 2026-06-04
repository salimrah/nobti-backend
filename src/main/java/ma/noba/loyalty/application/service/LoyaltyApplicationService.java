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
import ma.noba.loyalty.infra.JpaLoyaltyPointsTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoyaltyApplicationService {

    private final LoyaltyPointsRepository pointsRepository;
    private final JpaLoyaltyPointsTransactionRepository txnRepository;
    private final LoyaltyRewardRepository rewardRepository;
    private final LoyaltyRedemptionRepository redemptionRepository;

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
                    lp.getProviderId().toString(), lp.getProviderId().toString(), lp.getTotalPoints(),
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
            providerId.toString(), providerId.toString(), lp.getTotalPoints(), lp.getTier(),
            rewards.stream().map(r -> new LoyaltyRewardResponse(
                r.getId().toString(), r.getProviderId().toString(),
                r.getName(), r.getDescription(), r.getPointsRequired(),
                r.isActive(), r.getCreatedAt()
            )).toList(),
            history
        );
    }

    @Transactional
    public void awardPoints(UUID userId, UUID providerId, int points, String reason, UUID referenceId) {
        LoyaltyPoints lp = pointsRepository.findByUserIdAndProviderId(userId, providerId)
            .orElseGet(() -> pointsRepository.save(new LoyaltyPoints(userId, providerId)));

        lp.addPoints(points);
        pointsRepository.save(lp);

        LoyaltyPointsTransaction txn = new LoyaltyPointsTransaction(userId, providerId, points, reason, referenceId);
        txnRepository.save(txn);
    }

    @Transactional
    public RedeemRewardResponse redeemReward(UUID userId, UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
            .orElseThrow(() -> new IllegalArgumentException("Reward not found: " + rewardId));

        if (!reward.isActive()) {
            throw new IllegalStateException("Reward is no longer available");
        }

        LoyaltyPoints lp = pointsRepository.findByUserIdAndProviderId(userId, reward.getProviderId())
            .orElseThrow(() -> new IllegalStateException("No loyalty points for this provider"));

        lp.deductPoints(reward.getPointsRequired());
        pointsRepository.save(lp);

        LoyaltyPointsTransaction txn = new LoyaltyPointsTransaction(
            userId, reward.getProviderId(), -reward.getPointsRequired(),
            "REDEEMED:" + reward.getName(), rewardId
        );
        txnRepository.save(txn);

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
    public void deactivateReward(UUID rewardId) {
        LoyaltyReward reward = rewardRepository.findById(rewardId)
            .orElseThrow(() -> new IllegalArgumentException("Reward not found: " + rewardId));
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
