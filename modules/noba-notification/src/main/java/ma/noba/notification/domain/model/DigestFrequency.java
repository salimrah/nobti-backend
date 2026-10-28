package ma.noba.notification.domain.model;

/** How often a user wants non-urgent notifications bundled. */
public enum DigestFrequency {
    NONE,
    DAILY,
    WEEKLY;

    public static DigestFrequency parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
