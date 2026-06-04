package ma.noba.booking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AvailableSlotsResponse {
    private LocalDate date;
    private int dayOfWeek;
    private List<TimeSlot> slots;

    @Data
    @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;
    }
}
