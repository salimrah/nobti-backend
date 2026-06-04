package ma.noba.provider.domain.port.service;

import ma.noba.provider.domain.model.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository {

    Provider save(Provider provider);

    Optional<Provider> findById(UUID id);

    Optional<Provider> findByUserId(UUID userId);

    List<Provider> findAllById(Iterable<UUID> ids);

    List<Provider> findAllActive();

    Page<Provider> findAllActive(Pageable pageable);

    List<Provider> findByCategory(String category);

    Page<Provider> findByCategory(String category, Pageable pageable);

    Page<ProviderDistanceProjection> findNearbyIds(double lat, double lon, double radiusKm, Pageable pageable);

    Page<ProviderDistanceProjection> findNearbyIdsWithPostGis(double lat, double lon, double radiusKm, Pageable pageable);

    List<Provider> findAll();
}
