package ma.noba.admin;

import ma.noba.admin.application.dto.BulkUserActionRequest;
import ma.noba.admin.application.dto.CreateAdminRequest;
import ma.noba.admin.application.service.AdminAuditService;
import ma.noba.admin.application.service.AdminService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminInsightsPort;
import ma.noba.shared.domain.port.AdminPaymentPort;
import ma.noba.shared.domain.port.AdminProviderPort;
import ma.noba.shared.domain.port.AdminQueuePort;
import ma.noba.shared.domain.port.AdminRolePort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import ma.noba.shared.domain.port.AdminUserPort;
import ma.noba.shared.domain.port.ProviderDocumentPort;
import ma.noba.shared.domain.port.service.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock AdminIdentityPort identity;
    @Mock AdminQueuePort queues;
    @Mock AdminUserPort users;
    @Mock AdminProviderPort providers;
    @Mock AdminUserDirectoryPort directory;
    @Mock AdminAuditService audit;
    @Mock AdminPaymentPort payments;
    @Mock AdminRolePort roles;
    @Mock AdminInsightsPort insights;
    @Mock ProviderDocumentPort providerDocuments;
    @Mock ApiKeyRepository apiKeys;

    private AdminService service;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminService(identity, queues, users, providers, directory, audit, payments, roles,
                insights, providerDocuments, apiKeys);
    }

    @Test
    @DisplayName("Creating an administrator provisions the account and adds the ADMIN role")
    void createAdmin() {
        UUID newId = UUID.randomUUID();
        when(identity.createAdmin("new@noba.ma", "Passw0rd!123", "New", "Admin")).thenReturn(newId);
        when(roles.rolesOf(newId)).thenReturn(Set.of("USER"));
        when(identity.findUser(newId)).thenReturn(Optional.of(new AdminIdentityPort.UserState(
                newId, "ACTIVE", "new@noba.ma", null, Instant.now())));

        var view = service.createAdmin(actor, "ops@noba.ma",
                new CreateAdminRequest("new@noba.ma", "Passw0rd!123", "New", "Admin"));

        assertThat(view.email()).isEqualTo("new@noba.ma");
        verify(roles).replaceRoles(eq(newId), eq(Set.of("USER", "ADMIN")));
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("ADMIN_CREATED"), eq("USER"), eq(newId), any());
    }

    @Test
    @DisplayName("Revoking administrator access blocks self, non-admins and the last admin")
    void revokeAdminGuards() {
        UUID target = UUID.randomUUID();

        assertThatThrownBy(() -> service.revokeAdmin(actor, "ops", actor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own");

        when(roles.rolesOf(target)).thenReturn(Set.of("USER"));
        assertThatThrownBy(() -> service.revokeAdmin(actor, "ops", target))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not an administrator");

        when(roles.rolesOf(target)).thenReturn(Set.of("USER", "ADMIN"));
        when(roles.countAdmins()).thenReturn(1L);
        assertThatThrownBy(() -> service.revokeAdmin(actor, "ops", target))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last administrator");

        when(roles.countAdmins()).thenReturn(2L);
        service.revokeAdmin(actor, "ops", target);
        verify(roles).replaceRoles(eq(target), eq(Set.of("USER")));
        verify(identity).revokeSessions(target);
    }

    @Test
    @DisplayName("Bulk actions skip the acting administrator")
    void bulkSkipsSelf() {
        var results = service.bulkUserAction(actor, "ops",
                new BulkUserActionRequest(List.of(actor), "SUSPEND", "cleanup"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).success()).isFalse();
        verify(identity, never()).suspendUser(any());
    }

    @Test
    @DisplayName("User search forwards filters, parses status and clamps the page size")
    void searchUsers() {
        PagedResponse<AdminUserDirectoryPort.UserRow> page = PagedResponse.of(
                List.of(new AdminUserDirectoryPort.UserRow(
                        UUID.randomUUID(), "Amine", "Tazi", "a@noba.ma", "+212700000010",
                        UserStatus.ACTIVE, Set.of("USER"), Instant.now())),
                0, 100, 1);
        when(directory.searchUsers(eq("amine"), eq(UserStatus.ACTIVE), eq("ADMIN"), eq(0), eq(100)))
                .thenReturn(page);

        var result = service.searchUsers("amine", "active", "ADMIN", -3, 5000);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).roles()).contains("USER");
        verify(directory).searchUsers("amine", UserStatus.ACTIVE, "ADMIN", 0, 100);
    }

    @Test
    @DisplayName("An unknown status filter means 'all' instead of failing")
    void unknownStatus() {
        when(directory.searchUsers(anyString(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(PagedResponse.of(List.of(), 0, 20, 0));

        service.searchUsers("", "not-a-status", "", 0, 20);

        verify(directory).searchUsers("", null, "", 0, 20);
    }

    @Test
    @DisplayName("Provider search passes filters through with clamped paging")
    void searchProviders() {
        when(providers.searchProviders(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(PagedResponse.of(List.of(), 0, 100, 0));

        service.searchProviders("atlas", "PENDING", -1, 1000);

        verify(providers).searchProviders("atlas", "PENDING", 0, 100);
    }

    @Test
    @DisplayName("Suspending a user records the before/after status in the audit trail")
    void suspendUserAudits() {
        UUID userId = UUID.randomUUID();
        when(identity.findUser(userId)).thenReturn(Optional.of(
                new AdminIdentityPort.UserState(userId, "ACTIVE", "a@noba.ma", null, Instant.now())));

        service.suspendUser(actor, "ops@noba.ma", userId, "spam");

        verify(identity).suspendUser(userId);
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("USER_SUSPENDED"), eq("USER"),
                eq(userId), org.mockito.ArgumentMatchers.argThat(metadata ->
                        "ACTIVE".equals(metadata.get("previousStatus"))
                                && "DISABLED".equals(metadata.get("newStatus"))
                                && "spam".equals(metadata.get("reason"))));
    }

    @Test
    @DisplayName("Approving a provider records the change")
    void approveProviderAudits() {
        UUID providerId = UUID.randomUUID();
        when(providers.findProvider(providerId)).thenReturn(Optional.of(
                new AdminProviderPort.ProviderState(providerId, UUID.randomUUID(), "Atlas", "PENDING")));

        service.approveProvider(actor, "ops@noba.ma", providerId);

        verify(providers).approveProvider(providerId);
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("PROVIDER_APPROVED"), eq("PROVIDER"),
                eq(providerId), org.mockito.ArgumentMatchers.argThat(metadata ->
                        "PENDING".equals(metadata.get("previousStatus"))
                                && "ACTIVE".equals(metadata.get("newStatus"))
                                && "Atlas".equals(metadata.get("name"))));
    }

    @Test
    @DisplayName("The queue board resolves provider names")
    void queueBoardEnrichesProviderNames() {
        UUID queueId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        when(queues.searchQueues("ACTIVE", 0, 20)).thenReturn(PagedResponse.of(
                List.of(new AdminQueuePort.QueueRow(queueId, providerId, "ACTIVE", 4, 1, 5, Instant.now())),
                0, 20, 1));
        when(providers.names(List.of(providerId))).thenReturn(Map.of(providerId, "Atlas Barber Club"));

        var page = service.searchQueues("ACTIVE", 0, 20);

        assertThat(page.content().get(0).providerName()).isEqualTo("Atlas Barber Club");
        assertThat(page.content().get(0).waiting()).isEqualTo(4);
    }

    @Test
    @DisplayName("Pausing a queue records the transition")
    void pauseQueueAudits() {
        UUID queueId = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        AdminQueuePort.QueueDetail detail = new AdminQueuePort.QueueDetail(
                queueId, providerId, "ACTIVE", Instant.now(), Instant.now(), List.of());
        when(queues.queueDetail(queueId)).thenReturn(Optional.of(detail));

        service.pauseQueue(actor, "ops@noba.ma", queueId);

        verify(queues).pauseQueue(queueId);
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("QUEUE_PAUSED"), eq("QUEUE"),
                eq(queueId), org.mockito.ArgumentMatchers.argThat(metadata ->
                        "ACTIVE".equals(metadata.get("previousStatus"))
                                && "PAUSED".equals(metadata.get("newStatus"))));
    }

    @Test
    @DisplayName("An administrator cannot change their own roles")
    void cannotEditOwnRoles() {
        UUID userId = UUID.randomUUID();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.updateRoles(userId, "ops@noba.ma", userId, Set.of("USER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own roles");
    }

    @Test
    @DisplayName("The last administrator cannot be demoted")
    void cannotDemoteLastAdmin() {
        UUID userId = UUID.randomUUID();
        when(identity.findUser(userId)).thenReturn(Optional.of(
                new AdminIdentityPort.UserState(userId, "ACTIVE", "a@noba.ma", null, Instant.now())));
        when(roles.rolesOf(userId)).thenReturn(Set.of("USER", "ADMIN"));
        when(roles.assignableRoles()).thenReturn(Set.of("USER", "PROVIDER", "ADMIN", "DELIVERER"));
        when(roles.countAdmins()).thenReturn(1L);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.updateRoles(actor, "ops@noba.ma", userId, Set.of("USER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last administrator");
    }

    @Test
    @DisplayName("Role updates keep USER, normalise names and are audited")
    void updatesRoles() {
        UUID userId = UUID.randomUUID();
        when(identity.findUser(userId)).thenReturn(Optional.of(
                new AdminIdentityPort.UserState(userId, "ACTIVE", "a@noba.ma", null, Instant.now())));
        when(roles.assignableRoles()).thenReturn(Set.of("USER", "PROVIDER", "ADMIN", "DELIVERER"));
        when(roles.rolesOf(userId)).thenReturn(Set.of("USER"));
        when(directory.displayNames(any())).thenReturn(Map.of(userId, "Amine Tazi"));

        var result = service.updateRoles(actor, "ops@noba.ma", userId, Set.of("provider", "unknown"));

        verify(roles).replaceRoles(userId, Set.of("USER", "PROVIDER"));
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("USER_ROLES_CHANGED"), eq("USER"),
                eq(userId), any());
        assertThat(result.roles()).contains("USER");
    }

    @Test
    @DisplayName("CSV exports include a header and one line per row")
    void exportsCsv() {
        when(directory.searchUsers(anyString(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(PagedResponse.of(List.of(new AdminUserDirectoryPort.UserRow(
                        UUID.randomUUID(), "Amine", "Tazi", "a@noba.ma", "+212700000010",
                        UserStatus.ACTIVE, Set.of("USER"), Instant.now())), 0, 5000, 1));
        String users = service.exportUsers("", "", "");
        assertThat(users).startsWith("id,first_name,last_name,email,phone,status,roles,created_at");
        assertThat(users.lines().count()).isEqualTo(2);

        when(payments.searchTransactions(anyString(), anyInt(), anyInt()))
                .thenReturn(PagedResponse.of(List.of(), 0, 5000, 0));
        assertThat(service.exportTransactions("")).startsWith("id,user_id,provider_id,amount");
    }

    @Test
    @DisplayName("Force-advancing a queue records how many were serving")
    void advanceQueueAudits() {
        UUID queueId = UUID.randomUUID();
        when(queues.queueDetail(queueId)).thenReturn(Optional.of(
                new AdminQueuePort.QueueDetail(queueId, UUID.randomUUID(), "ACTIVE",
                        Instant.now(), Instant.now(), List.of())));

        service.advanceQueue(actor, "ops@noba.ma", queueId);

        verify(queues).advanceQueue(queueId);
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("QUEUE_ADVANCED"), eq("QUEUE"),
                eq(queueId), any());
    }
}
