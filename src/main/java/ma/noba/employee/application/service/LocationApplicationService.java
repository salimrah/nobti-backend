package ma.noba.employee.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.employee.application.dto.CreateLocationRequest;
import ma.noba.employee.application.dto.LocationResponse;
import ma.noba.employee.application.dto.UpdateLocationRequest;
import ma.noba.employee.domain.model.ProviderLocation;
import ma.noba.employee.domain.port.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationApplicationService {

    private final LocationRepository locationRepository;

    @Transactional
    public LocationResponse createLocation(UUID providerId, CreateLocationRequest request) {
        ProviderLocation location = new ProviderLocation(providerId, request.getName(), request.getAddress());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setPhone(request.getPhone());
        location = locationRepository.save(location);
        return toResponse(location);
    }

    @Transactional
    public LocationResponse updateLocation(UUID locationId, UpdateLocationRequest request) {
        ProviderLocation location = locationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
        location.update(request.getName(), request.getAddress(),
            request.getLatitude(), request.getLongitude(), request.getPhone());
        location = locationRepository.save(location);
        return toResponse(location);
    }

    public List<LocationResponse> getLocations(UUID providerId) {
        return locationRepository.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<LocationResponse> getActiveLocations(UUID providerId) {
        return locationRepository.findByProviderIdAndIsActive(providerId, true).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public void deactivateLocation(UUID locationId) {
        ProviderLocation location = locationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
        location.deactivate();
        locationRepository.save(location);
    }

    @Transactional
    public void activateLocation(UUID locationId) {
        ProviderLocation location = locationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
        location.activate();
        locationRepository.save(location);
    }

    private LocationResponse toResponse(ProviderLocation l) {
        return new LocationResponse(
            l.getId().toString(), l.getProviderId().toString(),
            l.getName(), l.getAddress(),
            l.getLatitude(), l.getLongitude(),
            l.getPhone(), l.isActive()
        );
    }
}
