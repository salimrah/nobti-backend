package ma.noba.notification;

import ma.noba.notification.application.service.NotificationTemplateRenderer;
import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateRendererTest {

    @Mock NotificationTemplateRepository templates;

    private NotificationTemplateRenderer renderer() {
        return new NotificationTemplateRenderer(templates);
    }

    @Test
    @DisplayName("Renders the exact locale and substitutes placeholders")
    void rendersExactLocale() {
        when(templates.findActive("reminder.service", "fr")).thenReturn(Optional.of(
                NotificationTemplate.of("reminder.service", "fr", 3, "Rappel",
                        "C'est l'heure de votre {{serviceName}} chez {{providerName}} !")));

        var rendered = renderer().render("reminder.service", "fr", Map.of(
                "serviceName", "Coupe", "providerName", "Atlas"), "fallback", "fallback");

        assertThat(rendered.title()).isEqualTo("Rappel");
        assertThat(rendered.body()).isEqualTo("C'est l'heure de votre Coupe chez Atlas !");
        assertThat(rendered.templateKey()).isEqualTo("reminder.service");
        assertThat(rendered.templateVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("Falls back from a regional locale to its language")
    void fallsBackToLanguage() {
        when(templates.findActive("queue.turn_start", "fr-ma")).thenReturn(Optional.empty());
        when(templates.findActive("queue.turn_start", "fr")).thenReturn(Optional.of(
                NotificationTemplate.of("queue.turn_start", "fr", 1, "C'est votre tour", "Venez vite")));

        var rendered = renderer().render("queue.turn_start", "fr-MA", Map.of(), "t", "b");

        assertThat(rendered.title()).isEqualTo("C'est votre tour");
    }

    @Test
    @DisplayName("Falls back to English when the locale is unknown")
    void fallsBackToEnglish() {
        when(templates.findActive("queue.waitlist_open", "es")).thenReturn(Optional.empty());
        when(templates.findActive("queue.waitlist_open", "en")).thenReturn(Optional.of(
                NotificationTemplate.of("queue.waitlist_open", "en", 1, "Open", "Join now")));

        var rendered = renderer().render("queue.waitlist_open", "es", Map.of(), "t", "b");

        assertThat(rendered.title()).isEqualTo("Open");
    }

    @Test
    @DisplayName("Uses the caller copy when no template exists")
    void fallsBackToCommandCopy() {
        when(templates.findActive("unknown.type", "en")).thenReturn(Optional.empty());

        var rendered = renderer().render("unknown.type", null, Map.of(), "Title", "Body");

        assertThat(rendered.title()).isEqualTo("Title");
        assertThat(rendered.body()).isEqualTo("Body");
        assertThat(rendered.templateKey()).isNull();
    }

    @Test
    @DisplayName("Missing variables render empty rather than leaking placeholders")
    void missingVariablesAreEmpty() {
        when(templates.findActive("queue.estimated_wait", "en")).thenReturn(Optional.of(
                NotificationTemplate.of("queue.estimated_wait", "en", 1, "Wait", "About {{minutes}} min")));

        var rendered = renderer().render("queue.estimated_wait", "en", Map.of(), "t", "b");

        assertThat(rendered.body()).isEqualTo("About  min");
        assertThat(rendered.body()).doesNotContain("{{");
    }
}
