package ma.noba.reminder.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateReminderRequest(
    @NotNull UUID providerId,
    UUID serviceId,
    @NotBlank String providerName,
    @NotBlank String serviceName,
    @NotNull @Min(1) Integer intervalDays
) {}
