package ma.noba.notification;

import ma.noba.notification.application.service.AdminCampaignService;
import ma.noba.notification.domain.model.NotificationCampaign;
import ma.noba.notification.domain.port.AdminCampaignRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminAudiencePort;
import ma.noba.shared.domain.port.AdminAuditPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCampaignServiceTest {

    @Mock AdminCampaignRepository campaigns;
    @Mock AdminAudiencePort audience;
    @Mock NotificationPort notifications;
    @Mock AdminAuditPort audit;

    private AdminCampaignService service;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdminCampaignService(campaigns, audience, notifications, audit);
        org.mockito.Mockito.lenient().when(campaigns.persist(any())).thenAnswer(invocation -> {
            NotificationCampaign campaign = invocation.getArgument(0);
            if (campaign.getId() == null) {
                var field = NotificationCampaign.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(campaign, UUID.randomUUID());
            }
            return campaign;
        });
    }

    @Test
    @DisplayName("Preview resolves the audience size")
    void preview() {
        when(audience.count(AdminAudiencePort.Audience.CLIENTS)).thenReturn(42L);
        assertThat(service.preview("clients")).isEqualTo(42);
    }

    @Test
    @DisplayName("A broadcast sends one notification per recipient and records the campaign")
    void sends() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(audience.count(AdminAudiencePort.Audience.PROVIDERS)).thenReturn(2L);
        when(audience.page(AdminAudiencePort.Audience.PROVIDERS, 0, 200)).thenReturn(List.of(first, second));
        when(audience.page(AdminAudiencePort.Audience.PROVIDERS, 1, 200)).thenReturn(List.of());

        var result = service.send(actor, "ops@noba.ma", "Holiday hours", "We open at 9.", "PROVIDERS");

        assertThat(result.sentCount()).isEqualTo(2);
        assertThat(result.status()).isEqualTo("SENT");
        verify(notifications, times(2)).send(any());
        verify(audit).record(eq(actor), eq("ops@noba.ma"), eq("BROADCAST_SENT"), eq("CAMPAIGN"),
                any(), any());
    }

    @Test
    @DisplayName("Empty copy or an empty audience is rejected")
    void validation() {
        assertThatThrownBy(() -> service.send(actor, "ops", " ", "body", "ALL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");

        when(audience.count(AdminAudiencePort.Audience.ALL)).thenReturn(0L);
        assertThatThrownBy(() -> service.send(actor, "ops", "Title", "Body", "ALL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("An unknown audience is rejected")
    void unknownAudience() {
        assertThatThrownBy(() -> service.preview("everyone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown audience");
    }

    @Test
    @DisplayName("Recent campaigns are paged")
    void recent() {
        when(campaigns.findAllByOrderByCreatedAtDesc(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        PagedResponse<?> page = service.recent(0, 20);
        assertThat(page.totalElements()).isZero();
    }
}
