package ma.noba.admin.application.dto;

import java.util.UUID;

public record BulkActionResult(UUID userId, boolean success, String error) {
}
