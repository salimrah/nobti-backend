package ma.noba.provider.interfaces.rest.profile;

import ma.noba.provider.application.dto.AccessibilityRequest;
import ma.noba.provider.application.dto.AddServiceRequest;
import ma.noba.provider.application.dto.BusinessHoursRequest;
import ma.noba.provider.application.dto.CreateProviderRequest;
import ma.noba.provider.application.dto.DayHours;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.application.dto.ProviderServiceResponse;
import ma.noba.provider.application.dto.ServiceCategoryResponse;
import ma.noba.provider.application.service.BusinessHoursService;
import ma.noba.provider.application.service.ProviderApplicationService;
import ma.noba.shared.domain.user.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers/self")
public class ProviderProfileController {

    private final ProviderApplicationService providerService;
    private final BusinessHoursService businessHoursService;

    public ProviderProfileController(ProviderApplicationService providerService, BusinessHoursService businessHoursService) {
        this.providerService = providerService;
        this.businessHoursService = businessHoursService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> createProvider(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CreateProviderRequest request) {
        ProviderResponse response = providerService.createProvider(user.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/providers/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<ProviderResponse> getMyProvider(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(providerService.getMyProvider(user.id()));
    }

    @PutMapping
    public ResponseEntity<ProviderResponse> updateProvider(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CreateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(user.id(), request));
    }

    @PostMapping("/services")
    public ResponseEntity<ProviderServiceResponse> addService(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AddServiceRequest request) {
        ProviderServiceResponse response = providerService.addService(user.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/providers/self/services/" + response.id())).body(response);
    }

    @GetMapping("/services")
    public ResponseEntity<List<ProviderServiceResponse>> getMyServices(@AuthenticationPrincipal UserPrincipal user) {
        ProviderResponse provider = providerService.getMyProvider(user.id());
        return ResponseEntity.ok(providerService.getServices(provider.id()));
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<Void> removeService(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID serviceId) {
        providerService.removeService(user.id(), serviceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/hours")
    public ResponseEntity<List<DayHours>> setBusinessHours(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BusinessHoursRequest request) {
        ProviderResponse provider = providerService.getMyProvider(user.id());
        List<DayHours> hours = businessHoursService.setBusinessHours(provider.id(), request);
        return ResponseEntity.ok(hours);
    }

    @GetMapping("/hours")
    public ResponseEntity<List<DayHours>> getBusinessHours(@AuthenticationPrincipal UserPrincipal user) {
        ProviderResponse provider = providerService.getMyProvider(user.id());
        return ResponseEntity.ok(businessHoursService.getBusinessHours(provider.id()));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ServiceCategoryResponse>> getCategories() {
        return ResponseEntity.ok(providerService.getAllCategories());
    }

    @PutMapping("/priority-queue")
    public ResponseEntity<ProviderResponse> togglePriorityQueue(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam boolean enabled) {
        providerService.togglePriorityQueue(user.id(), enabled);
        return ResponseEntity.ok(providerService.getMyProvider(user.id()));
    }

    @PutMapping("/accessibility")
    public ResponseEntity<ProviderResponse> updateAccessibility(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody AccessibilityRequest request) {
        return ResponseEntity.ok(providerService.updateAccessibility(user.id(), request));
    }
}
