package ma.noba.booking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class AvailabilitySlotResponse {
    private String id;
    private String providerId;
    private Integer dayOfWeek;
    private LocalDate specificDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean active;
}
