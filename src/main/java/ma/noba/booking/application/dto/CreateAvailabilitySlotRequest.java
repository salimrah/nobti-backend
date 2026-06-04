package ma.noba.booking.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class CreateAvailabilitySlotRequest {
    @NotNull
    private String providerId;

    private Integer dayOfWeek;

    private LocalDate specificDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
