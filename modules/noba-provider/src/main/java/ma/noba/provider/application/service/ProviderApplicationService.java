package ma.noba.provider.application.service;

import lombok.extern.slf4j.Slf4j;
import ma.noba.provider.application.dto.AccessibilityRequest;
import ma.noba.provider.application.dto.AddServiceRequest;
import ma.noba.provider.application.dto.CreateProviderRequest;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.application.dto.ProviderServiceResponse;
import ma.noba.provider.application.dto.ServiceCategoryResponse;
import ma.noba.provider.domain.model.Provider;
import ma.noba.provider.domain.model.ProviderFavorite;
import ma.noba.provider.domain.model.ProviderRating;
import ma.noba.provider.domain.port.service.ProviderFavoriteRepository;
import ma.noba.provider.domain.port.service.BusinessHoursRepository;
import ma.noba.provider.domain.port.service.ProviderProvisioningPort;
import ma.noba.provider.domain.port.service.ProviderRatingRepository;
import ma.noba.provider.domain.port.service.ProviderRepository;
import ma.noba.provider.domain.port.service.ServiceCategoryRepository;
import ma.noba.provider.domain.event.ProviderCreatedEvent;
import ma.noba.provider.domain.event.ProviderRatedEvent;
import ma.noba.provider.domain.event.ProviderStatusChangedEvent;
import ma.noba.provider.domain.event.ProviderUpdatedEvent;
import ma.noba.provider.domain.model.ProviderStatus;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.BusinessException;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.provider.domain.port.ProviderSearchPort;
import ma.noba.provider.domain.port.ProviderSubscriptionPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProviderApplicationService {

    private final ProviderRepository providerRepository;
    private final ProviderRatingRepository ratingRepository;
    private final ProviderFavoriteRepository favoriteRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProviderProvisioningPort provisioningPort;
    private final ProviderSubscriptionPort subscriptionPort;
    private final ProviderSearchPort providerSearchPort;
    private final BusinessHoursRepository businessHoursRepository;
    private final EventBus eventBus;

    public ProviderApplicationService(
            ProviderRepository providerRepository,
            ProviderRatingRepository ratingRepository,
            ProviderFavoriteRepository favoriteRepository,
            ServiceCategoryRepository serviceCategoryRepository,
            ProviderProvisioningPort provisioningPort,
            ProviderSubscriptionPort subscriptionPort,
            ProviderSearchPort providerSearchPort,
            BusinessHoursRepository businessHoursRepository,
            EventBus eventBus) {
        this.providerRepository = providerRepository;
        this.ratingRepository = ratingRepository;
        this.favoriteRepository = favoriteRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.provisioningPort = provisioningPort;
        this.subscriptionPort = subscriptionPort;
        this.providerSearchPort = providerSearchPort;
        this.businessHoursRepository = businessHoursRepository;
        this.eventBus = eventBus;
    }

    @Transactional
    @CacheEvict(value = "providers", allEntries = true)
    public ProviderResponse createProvider(UUID userId, CreateProviderRequest request) {
        log.info("Creating provider for user: {}", userId);
        if (providerRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException(ErrorCode.PROVIDER_ALREADY_EXISTS, "User already has a provider profile");
        }
        Provider provider = Provider.create(userId, request.name(), request.category());
        provider.updateInfo(request.name(), request.description(), null, request.address(), request.category());
        if (request.latitude() != null && request.longitude() != null) {
            provider.setLocation(request.latitude(), request.longitude());
        }
        Provider saved = providerRepository.save(provider);
        subscriptionPort.createFree(saved.getId());
        provisioningPort.onProviderCreated(saved.getUserId(), request.firstName(), request.lastName());
        eventBus.publish(new ProviderCreatedEvent(saved.getId(), saved.getUserId()));
        log.info("Provider created: {} for user: {}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "providerDetails", key = "#providerId")
    public ProviderResponse getProvider(UUID providerId) {
        log.debug("Fetching provider: {}", providerId);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });
        // Public discovery must not expose providers that are not yet approved (PENDING/REJECTED/
        // INACTIVE) or their coordinates. The owner uses /providers/self instead.
        if (!provider.isActive()) {
            log.debug("Hiding non-active provider {} from public discovery", providerId);
            throw new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
        }
        return toResponse(provider);
    }

    @Transactional(readOnly = true)
    public ProviderResponse getMyProvider(UUID userId) {
        log.debug("Fetching provider for user: {}", userId);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });
        return toResponse(provider);
    }

    @Transactional
    @CacheEvict(value = "providerDetails", key = "#result.id()")
    public ProviderResponse updateProvider(UUID userId, CreateProviderRequest request) {
        log.info("Updating provider for user: {}", userId);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for update by user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });

        provider.updateInfo(request.name(), request.description(), null, request.address(), request.category());
        if (request.latitude() != null && request.longitude() != null) {
            provider.setLocation(request.latitude(), request.longitude());
        }

        Provider saved = providerRepository.save(provider);
        eventBus.publish(new ProviderUpdatedEvent(saved.getId()));
        log.info("Provider updated: {} for user: {}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "providerDetails", allEntries = true)
    public ProviderServiceResponse addService(UUID userId, AddServiceRequest request) {
        log.info("Adding service for user: {}", userId);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for adding service by user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });

        int maxServices = subscriptionPort.maxServices(provider.getId());
        int currentServices = provider.getActiveServices().size();
        if (currentServices >= maxServices) {
            throw new BusinessException(ErrorCode.TIER_LIMIT_EXCEEDED,
                    "Your current plan allows a maximum of " + maxServices + " services. Upgrade to add more.");
        }

        provider.addService(request.name(), request.duration(), request.price(), request.category());
        Provider saved = providerRepository.save(provider);
        eventBus.publish(new ProviderUpdatedEvent(saved.getId()));

        var service = provider.getActiveServices().stream()
                .filter(s -> s.getName().equals(request.name()))
                .findFirst()
                .orElseThrow();

        return new ProviderServiceResponse(
                service.getId(),
                service.getName(),
                service.getDuration(),
                service.getPrice(),
                service.getCategory(),
                service.isActive()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "services", key = "#providerId")
    public List<ProviderServiceResponse> getServices(UUID providerId) {
        log.debug("Fetching services for provider: {}", providerId);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found for services: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });
        return provider.getActiveServices().stream()
                .map(s -> new ProviderServiceResponse(s.getId(), s.getName(), s.getDuration(), s.getPrice(), s.getCategory(), s.isActive()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "providerDetails", allEntries = true)
    public void removeService(UUID userId, UUID serviceId) {
        log.info("Removing service {} for user: {}", serviceId, userId);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for removing service by user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });

        provider.removeService(serviceId);
        Provider saved = providerRepository.save(provider);
        eventBus.publish(new ProviderUpdatedEvent(saved.getId()));
        log.info("Service {} removed from provider {}", serviceId, saved.getId());
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> searchByCategory(String category) {
        log.debug("Searching providers by category: {}", category);
        return providerRepository.findByCategory(category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProviderResponse> searchByCategory(String category, Pageable pageable) {
        return toPagedResponse(providerRepository.findByCategory(category, pageable));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProviderResponse> searchByText(String query, Pageable pageable) {
        return toPagedResponse(providerSearchPort.search(query, pageable));
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getAllActive() {
        return providerRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProviderResponse> getAllActive(Pageable pageable) {
        return toPagedResponse(providerRepository.findAllActive(pageable));
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getCustomerFavorites(UUID customerId) {
        List<ProviderFavorite> favorites = favoriteRepository.findByCustomerId(customerId);
        if (favorites.isEmpty()) return List.of();
        List<UUID> providerIds = favorites.stream()
                .map(ProviderFavorite::getProviderId)
                .toList();
        Map<UUID, Provider> providerMap = providerRepository.findAllById(providerIds).stream()
                .collect(Collectors.toMap(Provider::getId, p -> p));
        Map<UUID, Double> avgRatings = ratingRepository.getAverageRatings(providerIds);
        Map<UUID, Integer> ratingCounts = ratingRepository.countRatingsByProviderIds(providerIds);
        return favorites.stream()
                .map(fav -> providerMap.get(fav.getProviderId()))
                .filter(java.util.Objects::nonNull)
                .map(p -> toResponse(p, avgRatings, ratingCounts))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getAllCategories() {
        return serviceCategoryRepository.findAll().stream()
                .map(c -> new ServiceCategoryResponse(c.getId(), c.getName(), c.getDefaultDuration(), c.getDescription()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "ratings", key = "#providerId")
    public void rateProvider(UUID customerId, UUID providerId, int value, String comment) {
        log.info("Rating provider {} by customer {}: {} stars", providerId, customerId, value);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found for rating: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });

        if (provider.getUserId().equals(customerId)) {
            throw new IllegalArgumentException("You cannot rate your own provider profile");
        }

        var existing = ratingRepository.findByProviderIdAndCustomerId(providerId, customerId);
        if (existing.isPresent()) {
            existing.get().update(value, comment);
        } else {
            ratingRepository.save(new ProviderRating(providerId, customerId, value, comment));
        }
        eventBus.publish(new ProviderRatedEvent(providerId, customerId, value));
        log.info("Rating saved for provider {} by customer {}", providerId, customerId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "ratings", key = "#providerId")
    public double getAverageRating(UUID providerId) {
        log.debug("Getting average rating for provider: {}", providerId);
        return ratingRepository.getAverageRating(providerId);
    }

    @Transactional
    public void toggleFavorite(UUID customerId, UUID providerId, boolean favorite) {
        log.info("Toggling favorite for provider {} by customer {}: {}", providerId, customerId, favorite);
        providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found for favorite toggle: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });

        var existing = favoriteRepository.findByProviderIdAndCustomerId(providerId, customerId);
        if (favorite && existing.isEmpty()) {
            favoriteRepository.save(new ProviderFavorite(providerId, customerId));
        } else if (!favorite && existing.isPresent()) {
            favoriteRepository.deleteByProviderIdAndCustomerId(providerId, customerId);
        }
        log.debug("Favorite toggled for provider {} by customer {}: {}", providerId, customerId, favorite);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID customerId, UUID providerId) {
        log.debug("Checking favorite for provider {} by customer {}", providerId, customerId);
        return favoriteRepository.findByProviderIdAndCustomerId(providerId, customerId).isPresent();
    }

    @Transactional
    @CacheEvict(value = "providerDetails", key = "#providerId")
    public void togglePriorityQueue(UUID userId, boolean enabled) {
        log.info("Toggling priority queue for user {}: {}", userId, enabled);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for priority toggle by user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });
        provider.setPriorityQueueEnabled(enabled);
        providerRepository.save(provider);
        eventBus.publish(new ProviderUpdatedEvent(provider.getId()));
    }

    @Transactional
    @CacheEvict(value = "providerDetails", allEntries = true)
    public ProviderResponse updateAccessibility(UUID userId, AccessibilityRequest request) {
        log.info("Updating accessibility for user: {}", userId);
        Provider provider = providerRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("No provider profile found for accessibility update by user: {}", userId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "No provider profile found");
                });
        provider.setWheelchairAccessible(request.wheelchairAccessible());
        provider.setSignLanguage(request.signLanguage());
        provider.setSensoryFriendly(request.sensoryFriendly());
        provider.setUpdatedAt(Instant.now());
        Provider saved = providerRepository.save(provider);
        eventBus.publish(new ProviderUpdatedEvent(saved.getId()));
        return toResponse(saved);
    }

    @Transactional
    public void approveProvider(UUID providerId) {
        log.info("Approving provider: {}", providerId);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found for approval: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });
        ProviderStatus oldStatus = provider.getStatus();
        provider.setStatus(ProviderStatus.ACTIVE);
        provider.setVerified(true);
        provider.setVerificationDate(Instant.now());
        providerRepository.save(provider);
        eventBus.publish(new ProviderStatusChangedEvent(providerId, provider.getUserId(), oldStatus, ProviderStatus.ACTIVE));
        log.info("Provider approved: {}", providerId);
    }

    @Transactional
    public void rejectProvider(UUID providerId) {
        log.info("Rejecting provider: {}", providerId);
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.warn("Provider not found for rejection: {}", providerId);
                    return new NotFoundException(ErrorCode.PROVIDER_NOT_FOUND, "Provider not found");
                });
        ProviderStatus oldStatus = provider.getStatus();
        provider.setStatus(ProviderStatus.REJECTED);
        providerRepository.save(provider);
        eventBus.publish(new ProviderStatusChangedEvent(providerId, provider.getUserId(), oldStatus, ProviderStatus.REJECTED));
        log.info("Provider rejected: {}", providerId);
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> getPendingProviders() {
        log.debug("Fetching pending providers");
        return providerRepository.findByStatus(ProviderStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProviderResponse toResponse(Provider provider) {
        return toResponse(provider, Map.of(), Map.of());
    }

    private ProviderResponse toResponse(Provider provider, Map<UUID, Double> avgRatings, Map<UUID, Integer> ratingCounts) {
        String tier = subscriptionPort.tier(provider.getId());
        return new ProviderResponse(
                provider.getId(),
                provider.getUserId(),
                provider.getName(),
                provider.getDescription(),
                provider.getLogoUrl(),
                provider.getAddress(),
                provider.getLatitude(),
                provider.getLongitude(),
                provider.getCategory(),
                provider.getStatus(),
                provider.isVerified(),
                provider.isPriorityQueueEnabled(),
                provider.isWheelchairAccessible(),
                provider.isSignLanguage(),
                provider.isSensoryFriendly(),
                tier,
                avgRatings.getOrDefault(provider.getId(), ratingRepository.getAverageRating(provider.getId())),
                ratingCounts.getOrDefault(provider.getId(), (int) ratingRepository.countByProviderId(provider.getId())),
                isOpenNow(provider.getId())
        );
    }

    /** Whether the provider is open right now, based on today's business hours. */
    private boolean isOpenNow(UUID providerId) {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue();
        java.time.LocalTime now = java.time.LocalTime.now();
        return businessHoursRepository.findByProviderIdAndDayOfWeek(providerId, today)
                .map(hours -> hours.isOpenNow(now))
                .orElse(false);
    }

    private PagedResponse<ProviderResponse> toPagedResponse(Page<Provider> providerPage) {
        List<UUID> ids = providerPage.map(Provider::getId).toList();
        Map<UUID, Double> avgRatings = ratingRepository.getAverageRatings(ids);
        Map<UUID, Integer> ratingCounts = ratingRepository.countRatingsByProviderIds(ids);
        List<ProviderResponse> content = providerPage.getContent().stream()
                .map(p -> toResponse(p, avgRatings, ratingCounts))
                .toList();
        return PagedResponse.of(content, providerPage.getNumber(),
                providerPage.getSize(), providerPage.getTotalElements());
    }
}
