package ma.noba.packagegift.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.packagegift.application.dto.CreatePackageRequest;
import ma.noba.packagegift.application.dto.PackageResponse;
import ma.noba.packagegift.application.dto.UpdatePackageRequest;
import ma.noba.packagegift.application.service.PackageApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PackageController {

    private final PackageApplicationService packageService;
    private final ProviderLookupPort providerLookupPort;

    @PostMapping("/api/v1/providers/self/packages")
    public ResponseEntity<PackageResponse> createPackage(
        @AuthenticationPrincipal UUID userId,
        @Valid @RequestBody CreatePackageRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(packageService.createPackage(providerId, request));
    }

    @GetMapping("/api/v1/providers/{id}/packages")
    public ResponseEntity<List<PackageResponse>> getProviderPackages(@PathVariable UUID id) {
        return ResponseEntity.ok(packageService.getActiveProviderPackages(id));
    }

    @GetMapping("/api/v1/providers/self/packages")
    public ResponseEntity<List<PackageResponse>> getMyPackages(@AuthenticationPrincipal UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(packageService.getProviderPackages(providerId));
    }

    @PutMapping("/api/v1/providers/self/packages/{id}")
    public ResponseEntity<PackageResponse> updatePackage(
        @PathVariable UUID id,
        @Valid @RequestBody UpdatePackageRequest request
    ) {
        return ResponseEntity.ok(packageService.updatePackage(id, request));
    }

    @DeleteMapping("/api/v1/providers/self/packages/{id}")
    public ResponseEntity<Void> deactivatePackage(@PathVariable UUID id) {
        packageService.deactivatePackage(id);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
