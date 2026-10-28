package ma.noba.admin;

import ma.noba.admin.application.service.AdminService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminIdentityPort;
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

import java.util.List;
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

    private AdminService service;

    @BeforeEach
    void setUp() {
        service = new AdminService(identity, queues, users, providers, directory);
    }

    @Test
    @DisplayName("User search forwards filters, parses status and clamps the page size")
    void searchUsers() {
        PagedResponse<AdminUserDirectoryPort.UserRow> page = PagedResponse.of(
                List.of(new AdminUserDirectoryPort.UserRow(
                        UUID.randomUUID(), "Amine", "Tazi", "a@noba.ma", "+212700000010",
                        UserStatus.ACTIVE, Set.of("USER"), java.time.Instant.now())),
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
}
