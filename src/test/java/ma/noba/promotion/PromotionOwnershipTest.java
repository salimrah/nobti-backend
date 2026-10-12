package ma.noba.promotion;

import ma.noba.promotion.application.service.PromotionApplicationService;
import ma.noba.promotion.domain.model.Promotion;
import ma.noba.promotion.domain.port.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionOwnershipTest {

    @Mock PromotionRepository promotionRepository;

    private PromotionApplicationService service;

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PromotionApplicationService(promotionRepository);
    }

    private Promotion promotion() {
        return new Promotion(providerId, "Offer", "desc", new BigDecimal("10"),
                Instant.now(), Instant.now().plusSeconds(3600), 10, List.of());
    }

    @Test
    @DisplayName("A foreign provider cannot deactivate a promotion")
    void foreignProviderCannotDeactivatePromotion() {
        Promotion promotion = promotion();
        UUID id = UUID.randomUUID();
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));

        assertThatThrownBy(() -> service.deactivatePromotion(id, otherProviderId))
                .isInstanceOf(AccessDeniedException.class);

        verify(promotionRepository, never()).save(any());
    }

    @Test
    @DisplayName("The owning provider can deactivate a promotion")
    void owningProviderCanDeactivatePromotion() {
        Promotion promotion = promotion();
        UUID id = UUID.randomUUID();
        when(promotionRepository.findById(id)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenReturn(promotion);

        service.deactivatePromotion(id, providerId);

        assertThat(promotion.isActive()).isFalse();
    }
}
