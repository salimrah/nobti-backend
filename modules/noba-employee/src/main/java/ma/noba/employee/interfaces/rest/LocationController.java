package ma.noba.employee.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.employee.application.dto.CreateLocationRequest;
import ma.noba.employee.application.dto.LocationResponse;
import ma.noba.employee.application.dto.UpdateLocationRequest;
import ma.noba.employee.application.service.LocationApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers/self/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationApplicationService locationService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getLocations(@AuthenticationPrincipal(expression = "id") UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(locationService.getLocations(providerId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<LocationResponse>> getActiveLocations(@AuthenticationPrincipal(expression = "id") UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(locationService.getActiveLocations(providerId));
    }

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody CreateLocationRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(locationService.createLocation(providerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationResponse> updateLocation(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateLocationRequest request
    ) {
        return ResponseEntity.ok(locationService.updateLocation(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateLocation(@PathVariable UUID id) {
        locationService.deactivateLocation(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateLocation(@PathVariable UUID id) {
        locationService.activateLocation(id);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
