package ma.noba.subscription.application.service;

import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.subscription.application.dto.SubscriptionResponse;
import ma.noba.subscription.application.dto.TierInfoResponse;
import ma.noba.subscription.application.dto.UpdateSubscriptionRequest;
import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionTier;
import ma.noba.subscription.domain.port.ProviderSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionApplicationService {

    private final ProviderSubscriptionRepository subscriptionRepository;

    public SubscriptionApplicationService(ProviderSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public SubscriptionResponse createSubscription(UUID providerId, SubscriptionTier tier) {
        if (subscriptionRepository.findByProviderId(providerId).isPresent()) {
            throw new IllegalArgumentException("Provider already has a subscription");
        }
        ProviderSubscription subscription = new ProviderSubscription(providerId, tier);
        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID providerId) {
        ProviderSubscription subscription = subscriptionRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "No subscription found for provider"));
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public SubscriptionResponse updateSubscription(UUID providerId, UpdateSubscriptionRequest request) {
        ProviderSubscription subscription = subscriptionRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "No subscription found for provider"));

        if (request.tier() != null) {
            if (request.tier().ordinal() > subscription.getTier().ordinal()) {
                subscription.upgrade(request.tier());
            } else if (request.tier().ordinal() < subscription.getTier().ordinal()) {
                subscription.downgrade(request.tier());
            }
        }
        if (request.stripeSubscriptionId() != null) {
            subscription.setStripeSubscriptionId(request.stripeSubscriptionId());
        }
        subscription = subscriptionRepository.save(subscription);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void cancelSubscription(UUID providerId) {
        ProviderSubscription subscription = subscriptionRepository.findByProviderId(providerId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "No subscription found for provider"));
        subscription.cancel();
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public SubscriptionResponse getOrCreateDefault(UUID providerId) {
        return subscriptionRepository.findByProviderId(providerId)
                .map(SubscriptionResponse::from)
                .orElseGet(() -> createSubscription(providerId, SubscriptionTier.FREE));
    }

    @Transactional(readOnly = true)
    public List<TierInfoResponse> getTierInfo() {
        return TierInfoResponse.getAllTiers();
    }
}
