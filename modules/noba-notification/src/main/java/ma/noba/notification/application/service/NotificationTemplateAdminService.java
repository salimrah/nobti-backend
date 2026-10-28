package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.CreateTemplateRequest;
import ma.noba.notification.application.dto.TemplateResponse;
import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Admin workflow for notification copy. Editing copy always creates a new version; exactly one
 * version per key + locale is active, which is the one the renderer resolves.
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateAdminService {

    private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9._-]{2,79}$");
    private static final Pattern LOCALE = Pattern.compile("^[a-z]{2}(-[a-z0-9]{2,8})?$");
    private static final int MAX_SUBJECT = 200;
    private static final int MAX_BODY = 4000;

    private final NotificationTemplateRepository templates;

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(String key, String locale) {
        String keyFilter = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        String localeFilter = locale == null ? "" : locale.trim().toLowerCase(Locale.ROOT);
        return templates.findAllVersions().stream()
                .filter(t -> keyFilter.isEmpty() || t.getTemplateKey().contains(keyFilter))
                .filter(t -> localeFilter.isEmpty() || t.getLocale().equals(localeFilter))
                .map(TemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TemplateResponse> get(UUID id) {
        return templates.findTemplateById(id).map(TemplateResponse::from);
    }

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Template is required");
        }
        String key = normalizeKey(request.templateKey());
        String locale = normalizeLocale(request.locale());
        String body = request.body() == null ? "" : request.body().trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Template body is required");
        }
        if (body.length() > MAX_BODY) {
            throw new IllegalArgumentException("Template body must be at most " + MAX_BODY + " characters");
        }
        String subject = request.subject() == null ? null : request.subject().trim();
        if (subject != null && subject.length() > MAX_SUBJECT) {
            throw new IllegalArgumentException("Subject must be at most " + MAX_SUBJECT + " characters");
        }

        NotificationTemplate template = NotificationTemplate.version(
                key, locale, templates.nextVersion(key, locale), subject, body);
        boolean activate = request.active() == null || request.active();
        if (activate) {
            template.activate();
        } else {
            template.deactivate();
        }
        template = templates.saveTemplate(template);
        if (activate) {
            templates.deactivateOthers(key, locale, template.getId());
        }
        return TemplateResponse.from(template);
    }

    /** Switches the active version; activating one deactivates its siblings. */
    @Transactional
    public TemplateResponse setActive(UUID id, boolean active) {
        NotificationTemplate template = templates.findTemplateById(id)
                .orElseThrow(() -> new NoSuchElementException("Template not found"));
        if (active) {
            template.activate();
            template = templates.saveTemplate(template);
            templates.deactivateOthers(template.getTemplateKey(), template.getLocale(), template.getId());
        } else {
            template.deactivate();
            template = templates.saveTemplate(template);
        }
        return TemplateResponse.from(template);
    }

    private static String normalizeKey(String value) {
        String key = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "templateKey must be 3-80 characters of lowercase letters, digits, '.', '_' or '-'");
        }
        return key;
    }

    private static String normalizeLocale(String value) {
        String locale = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!LOCALE.matcher(locale).matches()) {
            throw new IllegalArgumentException("locale must look like 'en' or 'pt-br'");
        }
        return locale;
    }
}
