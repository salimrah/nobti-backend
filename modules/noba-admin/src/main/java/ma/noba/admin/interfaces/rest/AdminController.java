package ma.noba.admin.interfaces.rest;

import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.admin.application.service.AdminService;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.application.service.ProviderApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ProviderApplicationService providerApplicationService;

    public AdminController(AdminService adminService,
                           ProviderApplicationService providerApplicationService) {
        this.adminService = adminService;
        this.providerApplicationService = providerApplicationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId) {
        adminService.suspendUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/pending")
    public ResponseEntity<List<ProviderResponse>> getPendingProviders() {
        return ResponseEntity.ok(providerApplicationService.getPendingProviders());
    }

    @PutMapping("/providers/{providerId}/approve")
    public ResponseEntity<Void> approveProvider(@PathVariable UUID providerId) {
        adminService.approveProvider(providerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/providers/{providerId}/reject")
    public ResponseEntity<Void> rejectProvider(@PathVariable UUID providerId) {
        adminService.rejectProvider(providerId);
        return ResponseEntity.ok().build();
    }
}