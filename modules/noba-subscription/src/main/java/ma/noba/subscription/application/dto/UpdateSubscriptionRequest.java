package ma.noba.subscription.application.dto;

import ma.noba.subscription.domain.model.SubscriptionTier;

public record UpdateSubscriptionRequest(
        SubscriptionTier tier,
        String stripeSubscriptionId
) {}
