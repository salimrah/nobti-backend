package ma.noba.shared.domain.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses and evaluates the permissions attached to an {@link ApiKey}.
 * <p>
 * Permissions are stored as a comma-separated list (e.g. {@code "read,write"}). {@code *} is a
 * wildcard granting everything. A key with no permissions is treated as read-only (least
 * privilege) rather than full access.
 */
public final class ApiKeyPermissions {

    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String WILDCARD = "*";

    private ApiKeyPermissions() {
    }

    public static Set<String> parse(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return Set.of(READ);
        }
        Set<String> parsed = Arrays.stream(permissions.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(p -> !p.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return parsed.isEmpty() ? Set.of(READ) : parsed;
    }

    public static boolean allowsRead(Set<String> permissions) {
        return permissions.contains(WILDCARD)
                || permissions.contains(READ)
                || permissions.contains(WRITE);
    }

    public static boolean allowsWrite(Set<String> permissions) {
        return permissions.contains(WILDCARD) || permissions.contains(WRITE);
    }

    /** True for HTTP methods that only read state. */
    public static boolean isReadSafe(String httpMethod) {
        if (httpMethod == null) {
            return true;
        }
        return switch (httpMethod.toUpperCase()) {
            case "GET", "HEAD", "OPTIONS", "TRACE" -> true;
            default -> false;
        };
    }
}
