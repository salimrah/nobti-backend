package ma.noba.provider.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record BusinessHoursRequest(
        @Valid
        @NotNull(message = "Days list is required")
        List<DayHoursRequest> days
) {
    public record DayHoursRequest(
            @NotNull(message = "Day of week is required")
            int dayOfWeek,
            LocalTime opensAt,
            LocalTime closesAt,
            boolean closed
    ) {}
}
