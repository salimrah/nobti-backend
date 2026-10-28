package ma.noba.admin.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminProviderDetailView(
        UUID id,
        UUID userId,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        String name,
        String category,
        String address,
        String description,
        String status,
        boolean verified,
        Instant createdAt,
        List<Service> services) {

    public record Service(
            UUID id,
            String name,
            String category,
            long durationMinutes,
            BigDecimal price,
            boolean active) {
    }
}
