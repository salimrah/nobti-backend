package ma.noba.reminder.application.dto;

import jakarta.validation.constraints.Min;

public record UpdateReminderRequest(
    @Min(1) Integer intervalDays,
    Boolean active
) {}
