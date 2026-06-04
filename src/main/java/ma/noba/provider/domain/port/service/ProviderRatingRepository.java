package ma.noba.provider.domain.port.service;

import ma.noba.provider.domain.model.ProviderRating;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRatingRepository {

    ProviderRating save(ProviderRating rating);

    Optional<ProviderRating> findByProviderIdAndCustomerId(UUID providerId, UUID customerId);

    List<ProviderRating> findByProviderId(UUID providerId);

    double getAverageRating(UUID providerId);

    Map<UUID, Double> getAverageRatings(List<UUID> providerIds);

    Map<UUID, Integer> countRatingsByProviderIds(List<UUID> providerIds);
}
