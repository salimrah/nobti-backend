package ma.noba.admin.interfaces.rest;

import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.admin.application.service.AdminService;
import ma.noba.shared.domain.port.AdminProviderPort;
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


    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<ma.noba.shared.domain.port.AdminUserDirectoryPort.UserRow>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchUsers(query, status, role, page, size));
    }

    @GetMapping("/providers")
    public ResponseEntity<PagedResponse<ma.noba.shared.domain.port.AdminProviderPort.ProviderRow>> searchProviders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchProviders(query, status, page, size));
    }

    @GetMapping("/providers/pending")
    public ResponseEntity<List<AdminProviderPort.PendingProvider>> getPendingProviders() {
        return ResponseEntity.ok(adminService.pendingProviders());
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