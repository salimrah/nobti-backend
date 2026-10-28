package ma.noba.notification;

import ma.noba.notification.application.dto.CreateTemplateRequest;
import ma.noba.notification.application.service.NotificationTemplateAdminService;
import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateAdminServiceTest {

    @Mock NotificationTemplateRepository templates;

    private NotificationTemplateAdminService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateAdminService(templates);
    }

    private static void assignId(NotificationTemplate template) {
        try {
            Field id = NotificationTemplate.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(template, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void stubSave() {
        when(templates.saveTemplate(any())).thenAnswer(invocation -> {
            NotificationTemplate template = invocation.getArgument(0);
            if (template.getId() == null) {
                assignId(template);
            }
            return template;
        });
    }

    @Test
    @DisplayName("Publishing copy creates the next version and keeps one active")
    void createsNextActiveVersion() {
        stubSave();
        when(templates.nextVersion("queue.turn_start", "fr")).thenReturn(4);
        when(templates.deactivateOthers(anyString(), anyString(), any())).thenReturn(3);

        var response = service.create(new CreateTemplateRequest(
                "Queue.Turn_Start", "fr", "C'est votre tour", "Venez dans {{minutes}} minutes", true));

        assertThat(response.version()).isEqualTo(4);
        assertThat(response.templateKey()).isEqualTo("queue.turn_start");
        assertThat(response.locale()).isEqualTo("fr");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<UUID> keepId = ArgumentCaptor.forClass(UUID.class);
        verify(templates).deactivateOthers(anyString(), anyString(), keepId.capture());
        assertThat(keepId.getValue()).isEqualTo(response.id());
    }

    @Test
    @DisplayName("Publishing an inactive draft does not disturb the live version")
    void inactiveDraftKeepsLiveVersion() {
        stubSave();
        when(templates.nextVersion("promo.weekend", "en")).thenReturn(2);

        var response = service.create(new CreateTemplateRequest(
                "promo.weekend", "en", "Draft", "Draft body", false));

        assertThat(response.active()).isFalse();
        verify(templates, never()).deactivateOthers(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Activating a version deactivates its siblings")
    void activatingSwitchesVersion() {
        NotificationTemplate template = NotificationTemplate.of("reminder.service", "en", 1, "Old", "Body");
        assignId(template);
        when(templates.findTemplateById(template.getId())).thenReturn(Optional.of(template));
        when(templates.saveTemplate(template)).thenReturn(template);

        var response = service.setActive(template.getId(), true);

        assertThat(response.active()).isTrue();
        verify(templates).deactivateOthers("reminder.service", "en", template.getId());
    }

    @Test
    @DisplayName("Deactivating leaves the other versions untouched")
    void deactivatingDoesNotSwitch() {
        NotificationTemplate template = NotificationTemplate.of("reminder.service", "en", 1, "Old", "Body");
        assignId(template);
        when(templates.findTemplateById(template.getId())).thenReturn(Optional.of(template));
        when(templates.saveTemplate(template)).thenReturn(template);

        var response = service.setActive(template.getId(), false);

        assertThat(response.active()).isFalse();
        verify(templates, never()).deactivateOthers(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Invalid key, locale or body are rejected")
    void validation() {
        assertThatThrownBy(() -> service.create(new CreateTemplateRequest(
                "Bad Key!", "en", "s", "b", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateKey");

        assertThatThrownBy(() -> service.create(new CreateTemplateRequest(
                "valid.key", "english", "s", "b", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locale");

        assertThatThrownBy(() -> service.create(new CreateTemplateRequest(
                "valid.key", "en", "s", "  ", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body");
    }

    @Test
    @DisplayName("Listing filters by key and locale")
    void listFilters() {
        NotificationTemplate a = NotificationTemplate.of("queue.turn_start", "en", 1, "A", "b");
        NotificationTemplate b = NotificationTemplate.of("queue.turn_start", "fr", 1, "B", "b");
        NotificationTemplate c = NotificationTemplate.of("promo.weekend", "en", 1, "C", "b");
        when(templates.findAllVersions()).thenReturn(List.of(a, b, c));

        assertThat(service.list("queue", null)).hasSize(2);
        assertThat(service.list(null, "fr")).hasSize(1);
        assertThat(service.list("promo", "en")).hasSize(1);
        assertThat(service.list(null, null)).hasSize(3);
    }

    @Test
    @DisplayName("An unknown template id is reported as not found")
    void unknownTemplate() {
        UUID id = UUID.randomUUID();
        when(templates.findTemplateById(id)).thenReturn(Optional.empty());
        assertThat(service.get(id)).isEmpty();
        assertThatThrownBy(() -> service.setActive(id, true))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
