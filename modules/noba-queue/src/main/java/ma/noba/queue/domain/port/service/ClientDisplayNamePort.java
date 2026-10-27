package ma.noba.queue.domain.port.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves a human-readable display name for queue clients so the provider-facing queue view can
 * show a name instead of a raw user id. Only the provider (owner) endpoints surface these names.
 */
public interface ClientDisplayNamePort {

    /**
     * @param userIds client user ids
     * @return a map of user id to display name; ids with no resolvable name are omitted
     */
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);
}
