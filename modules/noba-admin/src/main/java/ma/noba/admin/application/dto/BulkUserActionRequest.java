package ma.noba.admin.application.dto;

import java.util.List;
import java.util.UUID;

/** SUSPEND or ACTIVATE applied to several accounts at once. */
public record BulkUserActionRequest(List<UUID> userIds, String action, String reason) {
}
