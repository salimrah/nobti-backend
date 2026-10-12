package ma.noba.provider.infra;

import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.provider.domain.port.service.ProviderDistanceProjection;
import ma.noba.provider.domain.port.service.ProviderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProviderRepository extends JpaRepository<Provider, UUID>, ProviderRepository {

    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT p FROM Provider p WHERE p.category = :category AND p.status = 'ACTIVE'")
    List<Provider> findByCategoryAndActive(@Param("category") String category);

    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT p FROM Provider p WHERE p.category = :category AND p.status = 'ACTIVE'")
    Page<Provider> findByCategoryAndActivePage(@Param("category") String category, Pageable pageable);

    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT p FROM Provider p WHERE p.userId = :userId")
    List<Provider> findByUserIdIgnoreStatus(@Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT p FROM Provider p WHERE p.status = 'ACTIVE'")
    List<Provider> findByActiveOnly();

    @EntityGraph(attributePaths = {"services"})
    @Query("SELECT p FROM Provider p WHERE p.status = 'ACTIVE'")
    Page<Provider> findByActiveOnlyPage(Pageable pageable);

    @EntityGraph(attributePaths = {"services"})
    Page<Provider> findByNameContainingIgnoreCaseAndStatus(String name, ProviderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"services"})
    Page<Provider> findByDescriptionContainingIgnoreCaseAndStatus(String description, ProviderStatus status, Pageable pageable);

    @Override
    default Page<Provider> searchActive(String query, Pageable pageable) {
        Page<Provider> byName = findByNameContainingIgnoreCaseAndStatus(query, ProviderStatus.ACTIVE, pageable);
        if (byName.hasContent()) {
            return byName;
        }
        return findByDescriptionContainingIgnoreCaseAndStatus(query, ProviderStatus.ACTIVE, pageable);
    }

    @Query("SELECT p FROM Provider p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Provider> findByStatusString(@Param("status") String status);

    @Query("SELECT COUNT(p) FROM Provider p WHERE p.status = :status")
    long countByStatusString(@Param("status") String status);

    @Override
    default List<Provider> findByStatus(ProviderStatus status) {
        return findByStatusString(status.name());
    }

    @Override
    default long countByStatus(ProviderStatus status) {
        return countByStatusString(status.name());
    }

    default Optional<Provider> findFirstByUserId(UUID userId) {
        List<Provider> providers = findByUserIdIgnoreStatus(userId);
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

    @Override
    default Optional<Provider> findByUserId(UUID userId) {
        return findFirstByUserId(userId);
    }

    @Override
    default List<Provider> findByCategory(String category) {
        return findByCategoryAndActive(category);
    }

    @Override
    default Page<Provider> findByCategory(String category, Pageable pageable) {
        return findByCategoryAndActivePage(category, pageable);
    }

    @Override
    default List<Provider> findAllActive() {
        return findByActiveOnly();
    }

    @Override
    default Page<Provider> findAllActive(Pageable pageable) {
        return findByActiveOnlyPage(pageable);
    }

    @Query(nativeQuery = true,
           value = "SELECT p.id, (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.latitude)))) AS dist_km FROM provider p WHERE p.status = 'ACTIVE' AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radiusKm ORDER BY dist_km",
            countQuery = "SELECT count(p.id) FROM provider p WHERE p.status = 'ACTIVE' AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * cos(radians(p.longitude) - radians(:lon)) + sin(radians(:lat)) * sin(radians(p.latitude)))) <= :radiusKm")
    Page<Object[]> findNearbyIdsNative(@Param("lat") double lat, @Param("lon") double lon, @Param("radiusKm") double radiusKm, Pageable pageable);

    @Query(nativeQuery = true,
           value = "SELECT p.id, (earth_distance(ll_to_earth(:lat, :lon), ll_to_earth(p.latitude, p.longitude)) / 1000) AS dist_km FROM provider p WHERE p.status = 'ACTIVE' AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND earth_box(ll_to_earth(:lat, :lon), :radiusMeters) @> ll_to_earth(p.latitude, p.longitude) AND earth_distance(ll_to_earth(:lat, :lon), ll_to_earth(p.latitude, p.longitude)) <= :radiusMeters ORDER BY dist_km",
            countQuery = "SELECT count(p.id) FROM provider p WHERE p.status = 'ACTIVE' AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL AND earth_box(ll_to_earth(:lat, :lon), :radiusMeters) @> ll_to_earth(p.latitude, p.longitude) AND earth_distance(ll_to_earth(:lat, :lon), ll_to_earth(p.latitude, p.longitude)) <= :radiusMeters")
    Page<Object[]> findNearbyIdsPostGis(@Param("lat") double lat, @Param("lon") double lon, @Param("radiusMeters") double radiusMeters, Pageable pageable);

    @Override
    default Page<ProviderDistanceProjection> findNearbyIds(double lat, double lon, double radiusKm, Pageable pageable) {
        return findNearbyIdsNative(lat, lon, radiusKm, pageable)
                .map(row -> new ProviderDistanceProjection() {
                    @Override
                    public UUID getId() { return (UUID) row[0]; }
                    @Override
                    public Double getDistanceKm() { return (Double) row[1]; }
                });
    }

    @Override
    default Page<ProviderDistanceProjection> findNearbyIdsWithPostGis(double lat, double lon, double radiusKm, Pageable pageable) {
        return findNearbyIdsPostGis(lat, lon, radiusKm * 1000, pageable)
                .map(row -> new ProviderDistanceProjection() {
                    @Override
                    public UUID getId() { return (UUID) row[0]; }
                    @Override
                    public Double getDistanceKm() { return (Double) row[1]; }
                });
    }
}
