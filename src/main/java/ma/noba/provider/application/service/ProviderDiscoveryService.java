package ma.noba.provider.application.service;

import ma.noba.provider.application.dto.NearbyProviderResponse;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.port.service.BusinessHoursRepository;
import ma.noba.provider.domain.port.service.ProviderDistanceProjection;
import ma.noba.provider.domain.port.service.ProviderRatingRepository;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.shared.application.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProviderDiscoveryService {

    private final ProviderRepository providerRepository;
    private final ProviderRatingRepository ratingRepository;
    private final BusinessHoursRepository businessHoursRepository;

    public ProviderDiscoveryService(ProviderRepository providerRepository,
                                    ProviderRatingRepository ratingRepository,
                                    BusinessHoursRepository businessHoursRepository) {
        this.providerRepository = providerRepository;
        this.ratingRepository = ratingRepository;
        this.businessHoursRepository = businessHoursRepository;
    }

    private PagedResponse<NearbyProviderResponse> buildNearbyResponse(
            Page<ProviderDistanceProjection> idAndDistancePage) {
        List<UUID> ids = idAndDistancePage.stream()
                .map(ProviderDistanceProjection::getId)
                .toList();
        Map<UUID, Double> distances = idAndDistancePage.stream()
                .collect(Collectors.toMap(ProviderDistanceProjection::getId, ProviderDistanceProjection::getDistanceKm));

        List<Provider> providers = providerRepository.findAllById(ids);

        Map<UUID, Double> avgRatings = ratingRepository.getAverageRatings(ids);
        Map<UUID, Integer> ratingCounts = ratingRepository.countRatingsByProviderIds(ids);

        List<NearbyProviderResponse> content = providers.stream()
                .map(p -> new NearbyProviderResponse(
                        p.getId(), p.getUserId(), p.getName(), p.getDescription(),
                        p.getLogoUrl(), p.getAddress(), p.getLatitude(), p.getLongitude(),
                        p.getCategory(), p.getStatus(), p.isVerified(),
                        avgRatings.getOrDefault(p.getId(), 0.0),
                        ratingCounts.getOrDefault(p.getId(), 0),
                        distances.getOrDefault(p.getId(), 0.0)
                ))
                .toList();

        return PagedResponse.of(content, idAndDistancePage.getNumber(),
                idAndDistancePage.getSize(), idAndDistancePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PagedResponse<NearbyProviderResponse> findNearby(BigDecimal lat, BigDecimal lon, double radiusKm, Pageable pageable) {
        Page<ProviderDistanceProjection> idAndDistancePage = providerRepository.findNearbyIds(
                lat.doubleValue(), lon.doubleValue(), radiusKm, pageable);
        return buildNearbyResponse(idAndDistancePage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NearbyProviderResponse> findNearbyOptimized(BigDecimal lat, BigDecimal lon, double radiusKm, Pageable pageable) {
        Page<ProviderDistanceProjection> idAndDistancePage = providerRepository.findNearbyIdsWithPostGis(
                lat.doubleValue(), lon.doubleValue(), radiusKm, pageable);
        return buildNearbyResponse(idAndDistancePage);
    }

    @Transactional(readOnly = true)
    public boolean isOpenNow(java.util.UUID providerId) {
        var now = LocalTime.now();
        var today = java.time.LocalDate.now().getDayOfWeek().getValue();

        return businessHoursRepository.findByProviderIdAndDayOfWeek(providerId, today)
                .map(hours -> hours.isOpenNow(now))
                .orElse(false);
    }
}
