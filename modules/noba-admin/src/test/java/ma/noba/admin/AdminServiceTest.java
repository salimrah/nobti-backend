package ma.noba.admin;

import ma.noba.admin.application.service.AdminAuditService;
import ma.noba.admin.application.service.AdminService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
import ma.noba.shared.domain.port.AdminPaymentPort;
import ma.noba.shared.domain.port.AdminProviderPort;
import ma.noba.shared.domain.port.AdminQueuePort;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import ma.noba.shared.domain.port.AdminUserPort;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

    private AdminService service;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminService(identity, queues, users, providers, directory, audit, payments);
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
                new AdminIdentityPort.UserState(userId, "ACTIVE")));

        service.suspendUser(actor, "ops@noba.ma", userId);

        verify(identity).suspendUser(userId);
        verify(audit).record(actor, "ops@noba.ma", "USER_SUSPENDED", "USER", userId,
                Map.of("previousStatus", "ACTIVE", "newStatus", "DISABLED"));
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
