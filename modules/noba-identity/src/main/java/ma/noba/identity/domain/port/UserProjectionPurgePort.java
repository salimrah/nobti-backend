package ma.noba.identity.domain.port;

import java.util.UUID;

/**
 * Identity-owned contract for removing a user's read-model projection. Implemented in the
 * composition root so identity does not depend on the user module.
 */
public interface UserProjectionPurgePort {

    void purge(UUID userId);
}
