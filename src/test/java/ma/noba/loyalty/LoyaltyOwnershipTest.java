package ma.noba.loyalty;

import ma.noba.loyalty.application.service.LoyaltyApplicationService;
import ma.noba.loyalty.domain.model.LoyaltyReward;
import ma.noba.loyalty.domain.port.LoyaltyPointsRepository;
import ma.noba.loyalty.domain.port.LoyaltyRedemptionRepository;
import ma.noba.loyalty.domain.port.LoyaltyRewardRepository;
import ma.noba.loyalty.infra.JpaLoyaltyPointsTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyOwnershipTest {

    @Mock LoyaltyPointsRepository pointsRepository;
    @Mock JpaLoyaltyPointsTransactionRepository txnRepository;
    @Mock LoyaltyRewardRepository rewardRepository;
    @Mock LoyaltyRedemptionRepository redemptionRepository;
    @Mock ma.noba.payment.domain.port.ProviderLookupPort providerLookupPort;

    private LoyaltyApplicationService service;

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LoyaltyApplicationService(pointsRepository, txnRepository, rewardRepository, redemptionRepository, providerLookupPort);
    }

    @Test
    @DisplayName("A foreign provider cannot deactivate a reward")
    void foreignProviderCannotDeactivateReward() {
        LoyaltyReward reward = new LoyaltyReward(providerId, "Free cut", "desc", 100);
        UUID rewardId = UUID.randomUUID();
        when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(reward));

        assertThatThrownBy(() -> service.deactivateReward(rewardId, otherProviderId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(reward.isActive()).isTrue();
        verify(rewardRepository, never()).save(any());
    }

    @Test
    @DisplayName("The owning provider can deactivate a reward")
    void owningProviderCanDeactivateReward() {
        LoyaltyReward reward = new LoyaltyReward(providerId, "Free cut", "desc", 100);
        UUID rewardId = UUID.randomUUID();
        when(rewardRepository.findById(rewardId)).thenReturn(Optional.of(reward));
        when(rewardRepository.save(any())).thenReturn(reward);

        service.deactivateReward(rewardId, providerId);

        assertThat(reward.isActive()).isFalse();
    }
}
