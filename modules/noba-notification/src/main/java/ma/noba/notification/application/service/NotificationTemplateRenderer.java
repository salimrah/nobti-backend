package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders notification copy from the template table. Placeholders use {@code {{name}}} and are
 * filled from the command data; unknown keys render as an empty string so users never see braces.
 * Falls back from the exact locale to the language prefix and finally to English, then to the
 * copy supplied by the caller when no template exists.
 */
@Component
@RequiredArgsConstructor
public class NotificationTemplateRenderer {

    private static final String FALLBACK_LOCALE = "en";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    private final NotificationTemplateRepository templates;

    public Rendered render(String type,
                           String locale,
                           Map<String, Object> data,
                           String fallbackTitle,
                           String fallbackBody) {
        for (String candidate : candidateLocales(locale)) {
            Optional<NotificationTemplate> match = templates.findActive(type, candidate);
            if (match.isPresent()) {
                NotificationTemplate template = match.get();
                return new Rendered(
                        substitute(template.getSubject() != null ? template.getSubject() : fallbackTitle, data),
                        substitute(template.getBody(), data),
                        template.getTemplateKey(),
                        template.getVersion());
            }
        }
        return new Rendered(fallbackTitle, fallbackBody, null, null);
    }

    private static Set<String> candidateLocales(String locale) {
        Set<String> candidates = new LinkedHashSet<>();
        if (locale != null && !locale.isBlank()) {
            String normalized = locale.trim().toLowerCase(Locale.ROOT);
            candidates.add(normalized);
            int separator = normalized.indexOf('-') >= 0 ? normalized.indexOf('-') : normalized.indexOf('_');
            if (separator > 0) {
                candidates.add(normalized.substring(0, separator));
            }
        }
        candidates.add(FALLBACK_LOCALE);
        return candidates;
    }

    private static String substitute(String template, Map<String, Object> data) {
        if (template == null) {
            return "";
        }
        if (data == null || data.isEmpty()) {
            return PLACEHOLDER.matcher(template).replaceAll("");
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Object value = data.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Rendered copy plus the template identity (null when the caller-supplied copy was used). */
    public record Rendered(String title, String body, String templateKey, Integer templateVersion) {
    }
}
