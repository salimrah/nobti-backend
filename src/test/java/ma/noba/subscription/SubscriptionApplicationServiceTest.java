package ma.noba.subscription;

import ma.noba.subscription.application.dto.UpdateSubscriptionRequest;
import ma.noba.subscription.application.service.SubscriptionApplicationService;
import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionTier;
import ma.noba.subscription.domain.port.ProviderSubscriptionRepository;
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
class SubscriptionApplicationServiceTest {

    @Mock ProviderSubscriptionRepository subscriptionRepository;

    private SubscriptionApplicationService service;

    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SubscriptionApplicationService(subscriptionRepository);
    }

    private ProviderSubscription existing(SubscriptionTier tier) {
        ProviderSubscription sub = new ProviderSubscription(providerId, tier);
        sub.setId(UUID.randomUUID());
        return sub;
    }

    @Test
    @DisplayName("A new subscription always starts on the FREE tier")
    void createAlwaysStartsFree() {
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> {
            ProviderSubscription sub = invocation.getArgument(0);
            if (sub.getId() == null) sub.setId(UUID.randomUUID());
            return sub;
        });

        var response = service.createSubscription(providerId);

        assertThat(response.tier()).isEqualTo(SubscriptionTier.FREE);
        assertThat(response.providerId()).isEqualTo(providerId.toString());
    }

    @Test
    @DisplayName("The client cannot escalate its subscription tier")
    void clientCannotEscalateTier() {
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.of(existing(SubscriptionTier.FREE)));

        assertThatThrownBy(() -> service.updateSubscription(providerId,
                new UpdateSubscriptionRequest(SubscriptionTier.ENTERPRISE, null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("The client cannot inject a stripeSubscriptionId")
    void clientCannotSetStripeSubscriptionId() {
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.of(existing(SubscriptionTier.FREE)));

        assertThatThrownBy(() -> service.updateSubscription(providerId,
                new UpdateSubscriptionRequest(null, "sub_attacker")))
                .isInstanceOf(AccessDeniedException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sending the same tier (or no change) is a harmless no-op")
    void sameTierAndNullRequestAreAllowed() {
        ProviderSubscription sub = existing(SubscriptionTier.FREE);
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.of(sub));

        assertThat(service.updateSubscription(providerId,
                new UpdateSubscriptionRequest(SubscriptionTier.FREE, null)).tier())
                .isEqualTo(SubscriptionTier.FREE);
        assertThat(service.updateSubscription(providerId, null).tier())
                .isEqualTo(SubscriptionTier.FREE);
    }

    @Test
    @DisplayName("getOrCreateDefault provisions FREE when none exists")
    void getOrCreateDefaultProvisionsFree() {
        when(subscriptionRepository.findByProviderId(providerId)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any())).thenAnswer(invocation -> {
            ProviderSubscription sub = invocation.getArgument(0);
            if (sub.getId() == null) sub.setId(UUID.randomUUID());
            return sub;
        });

        assertThat(service.getOrCreateDefault(providerId).tier()).isEqualTo(SubscriptionTier.FREE);
    }
}
