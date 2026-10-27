package ma.noba.subscription.application.service;

import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.subscription.application.dto.SubscriptionResponse;
import ma.noba.subscription.application.dto.TierInfoResponse;
import ma.noba.subscription.application.dto.UpdateSubscriptionRequest;
import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionTier;
import ma.noba.subscription.domain.port.ProviderSubscriptionRepository;
import org.springframework.security.access.AccessDeniedException;
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
    public SubscriptionResponse createSubscription(UUID providerId) {
        if (subscriptionRepository.findByProviderId(providerId).isPresent()) {
            throw new IllegalArgumentException("Provider already has a subscription");
        }
        // Entitlement is server-controlled: a new subscription always starts on the FREE tier.
        // Paid upgrades are applied only by a verified provider-side/webhook flow, never by the client.
        ProviderSubscription subscription = new ProviderSubscription(providerId, SubscriptionTier.FREE);
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

        // Entitlement is server-controlled. The client may neither change its tier nor inject a
        // Stripe subscription id; upgrades are applied only by a verified server-side/webhook flow.
        if (request != null && request.tier() != null && request.tier() != subscription.getTier()) {
            throw new AccessDeniedException("Subscription tier cannot be changed by the client");
        }
        if (request != null && request.stripeSubscriptionId() != null) {
            throw new AccessDeniedException("stripeSubscriptionId cannot be set by the client");
        }
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
                .orElseGet(() -> createSubscription(providerId));
    }

    @Transactional(readOnly = true)
    public List<TierInfoResponse> getTierInfo() {
        return TierInfoResponse.getAllTiers();
    }
}
