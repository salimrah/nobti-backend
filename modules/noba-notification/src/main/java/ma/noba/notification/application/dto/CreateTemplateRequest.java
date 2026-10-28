package ma.noba.notification.application.dto;

/**
 * Creates a new version of a template. Placeholders use {@code {{name}}} and are filled from the
 * notification data at send time.
 */
public record CreateTemplateRequest(
        String templateKey,
        String locale,
        String subject,
        String body,
        Boolean active
) {
}
