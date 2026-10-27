package ma.noba.provider.interfaces.rest.discovery;

import ma.noba.provider.application.dto.NearbyProviderResponse;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.application.dto.ProviderServiceResponse;
import ma.noba.provider.application.service.ProviderApplicationService;
import ma.noba.provider.application.service.ProviderDiscoveryService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Provider Discovery", description = "Public provider search and discovery")
public class ProviderDiscoveryController {

    private final ProviderApplicationService providerService;
    private final ProviderDiscoveryService discoveryService;

    public ProviderDiscoveryController(ProviderApplicationService providerService,
                                       ProviderDiscoveryService discoveryService) {
        this.providerService = providerService;
        this.discoveryService = discoveryService;
    }

    @Operation(summary = "Get all active providers (paginated)")
    @GetMapping
    public ResponseEntity<PagedResponse<ProviderResponse>> getAllProviders(Pageable pageable) {
        return ResponseEntity.ok(providerService.getAllActive(pageable));
    }

    @Operation(summary = "Get provider by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getProvider(@PathVariable UUID id) {
        return ResponseEntity.ok(providerService.getProvider(id));
    }

    @Operation(summary = "Get a provider's active services")
    @GetMapping("/{id}/services")
    public ResponseEntity<java.util.List<ProviderServiceResponse>> getProviderServices(@PathVariable UUID id) {
        return ResponseEntity.ok(providerService.getServices(id));
    }

    @Operation(summary = "Get customer's favorite providers")
    @GetMapping("/favorites")
    public ResponseEntity<List<ProviderResponse>> getFavorites(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(providerService.getCustomerFavorites(user.id()));
    }

    @Operation(summary = "Search providers by free text and/or category (paginated)")
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProviderResponse>> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        // Free-text search takes precedence; otherwise fall back to category; otherwise all active.
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(providerService.searchByText(q.trim(), pageable));
        }
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(providerService.searchByCategory(category, pageable));
        }
        return ResponseEntity.ok(providerService.getAllActive(pageable));
    }

    @GetMapping("/nearby")
    public ResponseEntity<PagedResponse<NearbyProviderResponse>> getNearbyProviders(
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lon,
            @RequestParam(defaultValue = "10") double radiusKm,
            @RequestParam(defaultValue = "false") boolean usePostGis,
            Pageable pageable) {
        if (usePostGis) {
            return ResponseEntity.ok(discoveryService.findNearbyOptimized(lat, lon, radiusKm, pageable));
        }
        return ResponseEntity.ok(discoveryService.findNearby(lat, lon, radiusKm, pageable));
    }
}
