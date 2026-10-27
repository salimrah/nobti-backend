package ma.noba.loyalty;

import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.loyalty.domain.model.LoyaltyReward;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import ma.noba.loyalty.domain.port.LoyaltyRedemptionRepository;
import ma.noba.loyalty.domain.port.LoyaltyRewardRepository;
import ma.noba.loyalty.domain.port.LoyaltyPointsTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyApplicationServiceTest {

    @Mock LoyaltyPointsRepository pointsRepository;
    @Mock LoyaltyPointsTransactionRepository txnRepository;
    @Mock LoyaltyRewardRepository rewardRepository;
    @Mock LoyaltyRedemptionRepository redemptionRepository;
    @Mock ma.noba.payment.domain.port.ProviderLookupPort providerLookupPort;

    private LoyaltyApplicationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private final UUID rewardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LoyaltyApplicationService(pointsRepository, txnRepository, rewardRepository, redemptionRepository, providerLookupPort);
    }

    @Test
    @DisplayName("Redemption is rejected when the atomic balance decrement affects no row (insufficient/overspend)")
    void redeemRejectedWhenBalanceInsufficient() {
        LoyaltyReward reward = new LoyaltyReward(providerId, "Free cut", "desc", 100);
        when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(reward));
        // Atomic conditional decrement returns 0 → not enough points (or lost a concurrent race).
        when(pointsRepository.deductPointsIfSufficient(userId, providerId, 100)).thenReturn(0);

        assertThatThrownBy(() -> service.redeemReward(userId, rewardId))
                .isInstanceOf(IllegalStateException.class);

        verify(txnRepository, never()).saveTransaction(any());
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Loyalty responses use the provider display name, not its id")
    void loyaltyUsesProviderName() {
        when(pointsRepository.findByUserId(userId)).thenReturn(List.of(new ma.noba.loyalty.domain.model.LoyaltyPoints(userId, providerId)));
        when(txnRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        when(rewardRepository.findByProviderIdAndIsActive(providerId, true)).thenReturn(List.of());
        when(providerLookupPort.findProviderName(providerId)).thenReturn(Optional.of("Salon Zineb"));

        var response = service.getMyLoyalty(userId);

        assertThat(response.getProviders().get(0).getProviderName()).isEqualTo("Salon Zineb");
    }

    @Test
    @DisplayName("Awarding points uses the atomic upsert-increment (no read-modify-write)")
    void awardPointsUsesAtomicIncrement() {
        service.awardPoints(userId, providerId, 10, "SERVICE_COMPLETED", null);

        verify(pointsRepository).addPoints(userId, providerId, 10);
        verify(txnRepository).saveTransaction(any());
    }

    @Test
    @DisplayName("Awarding zero/negative points is a no-op")
    void awardNonPositiveIsNoOp() {
        service.awardPoints(userId, providerId, 0, "SERVICE_COMPLETED", null);

        verify(pointsRepository, never()).addPoints(any(), any(), anyInt());
    }
}
