package ma.noba.admin.interfaces.rest;

import jakarta.validation.Valid;
import ma.noba.admin.application.dto.AdminAccountView;
import ma.noba.admin.application.dto.AdminApiKeyView;
import ma.noba.admin.application.dto.BulkActionResult;
import ma.noba.admin.application.dto.BulkUserActionRequest;
import ma.noba.admin.application.dto.CreateApiKeyRequest;
import ma.noba.admin.application.dto.CreatedApiKeyResponse;
import ma.noba.admin.application.dto.AdminAuditResponse;
import ma.noba.admin.application.dto.CreateAdminRequest;
import ma.noba.admin.application.dto.AdminPayoutView;
import ma.noba.admin.application.dto.AdminProviderDetailView;
import ma.noba.admin.application.dto.AdminProviderDocumentView;
import ma.noba.admin.application.dto.ReviewDocumentRequest;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminInsightsPort;
import ma.noba.admin.application.dto.AdminTransactionDetailView;
import ma.noba.admin.application.dto.AdminTransactionView;
import ma.noba.admin.application.dto.RefundRequest;
import ma.noba.admin.application.dto.AdminUserDetailView;
import ma.noba.admin.application.dto.UpdateUserRolesRequest;
import ma.noba.admin.application.dto.AdminQueueDetailView;
import ma.noba.admin.application.dto.AdminQueueView;
import ma.noba.admin.application.dto.AdminStatsResponse;
import ma.noba.admin.application.service.AdminService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminProviderPort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<AdminUserDirectoryPort.UserRow>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchUsers(query, status, role, page, size));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailView> userDetail(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.userDetail(userId));
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<AdminUserDetailView> updateRoles(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable UUID userId,
                                                           @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(
                adminService.updateRoles(principal.id(), principal.email(), userId, request.roles()));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminAccountView>> listAdmins() {
        return ResponseEntity.ok(adminService.listAdmins());
    }

    @PostMapping("/admins")
    public ResponseEntity<AdminAccountView> createAdmin(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(adminService.createAdmin(principal.id(), principal.email(), request));
    }

    @DeleteMapping("/admins/{userId}")
    public ResponseEntity<Void> revokeAdmin(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID userId) {
        adminService.revokeAdmin(principal.id(), principal.email(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<java.util.List<AdminIdentityPort.SessionRow>> sessions(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.sessions(userId));
    }

    @DeleteMapping("/users/{userId}/sessions")
    public ResponseEntity<Void> revokeSessions(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable UUID userId) {
        adminService.revokeSessions(principal.id(), principal.email(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/bulk")
    public ResponseEntity<List<BulkActionResult>> bulkUserAction(@AuthenticationPrincipal UserPrincipal principal,
                                                                 @RequestBody BulkUserActionRequest request) {
        return ResponseEntity.ok(adminService.bulkUserAction(principal.id(), principal.email(), request));
    }

    @PostMapping("/providers/reindex")
    public ResponseEntity<java.util.Map<String, Boolean>> reindexProviders(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(java.util.Map.of(
                "reindexed", adminService.reindexProviders(principal.id(), principal.email())));
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<AdminApiKeyView>> listApiKeys() {
        return ResponseEntity.ok(adminService.listApiKeys());
    }

    @PostMapping("/api-keys")
    public ResponseEntity<CreatedApiKeyResponse> createApiKey(@AuthenticationPrincipal UserPrincipal principal,
                                                              @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.ok(adminService.createApiKey(principal.id(), principal.email(), request));
    }

    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<Void> revokeApiKey(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable UUID keyId) {
        adminService.revokeApiKey(principal.id(), principal.email(), keyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/insights")
    public ResponseEntity<AdminInsightsPort.Insights> insights(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.insights(days));
    }

    @PutMapping("/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID userId,
                                            @RequestParam(required = false) String reason) {
        adminService.suspendUser(principal.id(), principal.email(), userId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/activate")
    public ResponseEntity<Void> activateUser(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable UUID userId) {
        adminService.activateUser(principal.id(), principal.email(), userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers")
    public ResponseEntity<PagedResponse<AdminProviderPort.ProviderRow>> searchProviders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchProviders(query, status, page, size));
    }

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<AdminProviderDetailView> providerDetail(@PathVariable UUID providerId) {
        return ResponseEntity.ok(adminService.providerDetail(providerId));
    }

    @PutMapping("/providers/{providerId}/suspend")
    public ResponseEntity<Void> suspendProvider(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID providerId) {
        adminService.suspendProvider(principal.id(), principal.email(), providerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/providers/{providerId}/reactivate")
    public ResponseEntity<Void> reactivateProvider(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable UUID providerId) {
        adminService.reactivateProvider(principal.id(), principal.email(), providerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/{providerId}/documents")
    public ResponseEntity<List<AdminProviderDocumentView>> providerDocuments(@PathVariable UUID providerId) {
        return ResponseEntity.ok(adminService.providerDocuments(providerId));
    }

    @GetMapping("/documents/pending")
    public ResponseEntity<List<AdminProviderDocumentView>> pendingDocuments() {
        return ResponseEntity.ok(adminService.pendingDocuments());
    }

    @PostMapping("/documents/{documentId}/review")
    public ResponseEntity<AdminProviderDocumentView> reviewDocument(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID documentId,
            @RequestBody ReviewDocumentRequest request) {
        return ResponseEntity.ok(adminService.reviewDocument(principal.id(), principal.email(),
                documentId, request.status(), request.note()));
    }

    @GetMapping("/providers/pending")
    public ResponseEntity<List<AdminProviderPort.PendingProvider>> getPendingProviders() {
        return ResponseEntity.ok(adminService.pendingProviders());
    }

    @PutMapping("/providers/{providerId}/approve")
    public ResponseEntity<Void> approveProvider(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable UUID providerId) {
        adminService.approveProvider(principal.id(), principal.email(), providerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/providers/{providerId}/reject")
    public ResponseEntity<Void> rejectProvider(@AuthenticationPrincipal UserPrincipal principal,
                                               @PathVariable UUID providerId) {
        adminService.rejectProvider(principal.id(), principal.email(), providerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/queues")
    public ResponseEntity<PagedResponse<AdminQueueView>> searchQueues(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchQueues(status, page, size));
    }

    @GetMapping("/queues/{queueId}")
    public ResponseEntity<AdminQueueDetailView> queueDetail(@PathVariable UUID queueId) {
        return ResponseEntity.ok(adminService.queueDetail(queueId));
    }

    @PutMapping("/queues/{queueId}/pause")
    public ResponseEntity<Void> pauseQueue(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable UUID queueId) {
        adminService.pauseQueue(principal.id(), principal.email(), queueId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/queues/{queueId}/resume")
    public ResponseEntity<Void> resumeQueue(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID queueId) {
        adminService.resumeQueue(principal.id(), principal.email(), queueId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/queues/{queueId}/close")
    public ResponseEntity<Void> closeQueue(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable UUID queueId) {
        adminService.closeQueue(principal.id(), principal.email(), queueId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/queues/{queueId}/next")
    public ResponseEntity<Void> advanceQueue(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable UUID queueId) {
        adminService.advanceQueue(principal.id(), principal.email(), queueId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payments/transactions")
    public ResponseEntity<PagedResponse<AdminTransactionView>> searchTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchTransactions(status, page, size));
    }

    @GetMapping("/payments/transactions/{transactionId}")
    public ResponseEntity<AdminTransactionDetailView> transactionDetail(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(adminService.transactionDetail(transactionId));
    }

    @PostMapping("/payments/transactions/{transactionId}/refund")
    public ResponseEntity<Void> refundTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable UUID transactionId,
                                                  @RequestBody(required = false) RefundRequest request) {
        adminService.refundTransaction(principal.id(), principal.email(), transactionId,
                request == null ? null : request.amount(),
                request == null ? null : request.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payments/payouts/{payoutId}/retry")
    public ResponseEntity<Void> retryPayout(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable UUID payoutId) {
        adminService.retryPayout(principal.id(), principal.email(), payoutId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payments/payouts")
    public ResponseEntity<PagedResponse<AdminPayoutView>> searchPayouts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.searchPayouts(status, page, size));
    }

    @GetMapping(value = "/exports/audit.csv", produces = "text/csv")
    public ResponseEntity<String> exportAudit(@RequestParam(required = false) String action,
                                              @RequestParam(required = false) String targetType) {
        return csv("audit.csv", adminService.exportAudit(action, targetType));
    }

    @GetMapping(value = "/exports/users.csv", produces = "text/csv")
    public ResponseEntity<String> exportUsers(@RequestParam(required = false) String query,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String role) {
        return csv("users.csv", adminService.exportUsers(query, status, role));
    }

    @GetMapping(value = "/exports/transactions.csv", produces = "text/csv")
    public ResponseEntity<String> exportTransactions(@RequestParam(required = false) String status) {
        return csv("transactions.csv", adminService.exportTransactions(status));
    }

    @GetMapping(value = "/exports/payouts.csv", produces = "text/csv")
    public ResponseEntity<String> exportPayouts(@RequestParam(required = false) String status) {
        return csv("payouts.csv", adminService.exportPayouts(status));
    }

    private static ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/audit")
    public ResponseEntity<PagedResponse<AdminAuditResponse>> audit(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.audit(action, targetType, page, size));
    }
}
