package ma.noba.provider.application.dto;

import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.subscription.domain.model.SubscriptionTier;

import java.util.UUID;

public record ProviderResponse(
        UUID id,
        UUID userId,
        String name,
        String description,
        String logoUrl,
        String address,
        Double latitude,
        Double longitude,
        String category,
        ProviderStatus status,
        boolean verified,
        boolean priorityQueueEnabled,
        boolean wheelchairAccessible,
        boolean signLanguage,
        boolean sensoryFriendly,
        SubscriptionTier subscriptionTier,
        double averageRating,
        int ratingCount,
        boolean openNow
) {}
