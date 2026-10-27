package ma.noba.provider.application.dto;

import ma.noba.provider.domain.model.ProviderStatus;

import java.util.UUID;

public record NearbyProviderResponse(
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
        double averageRating,
        int ratingCount,
        double distanceKm
) {}
