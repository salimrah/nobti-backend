package ma.noba.shared.domain.port;

import java.util.List;
import java.util.UUID;

/** Resolves broadcast audiences without exposing user tables to the admin module. */
public interface AdminAudiencePort {

    long count(Audience audience);

    List<UUID> page(Audience audience, int page, int size);

    enum Audience {
        ALL,
        CLIENTS,
        PROVIDERS
    }
}
