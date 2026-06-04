package ma.noba.reminder.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReminderResponse(
    UUID id,
    UUID providerId,
    UUID serviceId,
    String providerName,
    String serviceName,
    Integer intervalDays,
    Instant nextReminderAt,
    boolean active,
    Instant createdAt
) {}
