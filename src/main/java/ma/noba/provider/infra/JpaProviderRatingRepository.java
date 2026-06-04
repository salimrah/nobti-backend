package ma.noba.provider.infra;

import ma.noba.provider.domain.model.ProviderRating;
import ma.noba.provider.domain.port.service.ProviderRatingRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface JpaProviderRatingRepository extends JpaRepository<ProviderRating, UUID>, ProviderRatingRepository {

    Optional<ProviderRating> findByProviderIdAndCustomerId(UUID providerId, UUID customerId);

    List<ProviderRating> findByProviderId(UUID providerId);

    @Query("SELECT AVG(r.value) FROM ProviderRating r WHERE r.providerId = :providerId")
    Optional<Double> avgByProviderId(UUID providerId);

    @Override
    default double getAverageRating(UUID providerId) {
        return avgByProviderId(providerId).orElse(0.0);
    }

    @Query("SELECT r.providerId, AVG(r.value) FROM ProviderRating r WHERE r.providerId IN :ids GROUP BY r.providerId")
    List<Object[]> findAverageRatingsByProviderIds(@Param("ids") List<UUID> providerIds);

    @Query("SELECT r.providerId, COUNT(r) FROM ProviderRating r WHERE r.providerId IN :ids GROUP BY r.providerId")
    List<Object[]> countRatingsByProviderIdsGrouped(@Param("ids") List<UUID> providerIds);

    @Override
    default Map<UUID, Double> getAverageRatings(List<UUID> providerIds) {
        if (providerIds.isEmpty()) return Map.of();
        return findAverageRatingsByProviderIds(providerIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Double) row[1]
                ));
    }

    @Override
    default Map<UUID, Integer> countRatingsByProviderIds(List<UUID> providerIds) {
        if (providerIds.isEmpty()) return Map.of();
        return countRatingsByProviderIdsGrouped(providerIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).intValue()
                ));
    }
}
