package ma.noba.subscription.application.dto;

import ma.noba.subscription.domain.model.ProviderSubscription;
import ma.noba.subscription.domain.model.SubscriptionStatus;
import ma.noba.subscription.domain.model.SubscriptionTier;

import java.time.Instant;

public record SubscriptionResponse(
        String id,
        String providerId,
        SubscriptionTier tier,
        SubscriptionStatus status,
        Instant startedAt,
        Instant renewsAt,
        String stripeSubscriptionId,
        int maxServices,
        boolean hasAnalyticsAccess,
        boolean hasPromotionsAccess,
        boolean hasBadgeAccess,
        boolean hasFeaturedPlacement,
        boolean hasMultiLocationAccess,
        boolean hasEmployeeManagementAccess,
        boolean hasApiAccess
) {
    public static SubscriptionResponse from(ProviderSubscription sub) {
        return new SubscriptionResponse(
                sub.getId().toString(),
                sub.getProviderId().toString(),
                sub.getTier(),
                sub.getStatus(),
                sub.getStartedAt(),
                sub.getRenewsAt(),
                sub.getStripeSubscriptionId(),
                sub.getMaxServices(),
                sub.hasAnalyticsAccess(),
                sub.hasPromotionsAccess(),
                sub.hasBadgeAccess(),
                sub.hasFeaturedPlacement(),
                sub.hasMultiLocationAccess(),
                sub.hasEmployeeManagementAccess(),
                sub.hasApiAccess()
        );
    }
}
