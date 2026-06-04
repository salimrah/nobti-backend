package ma.noba.provider.application.dto;

import java.time.LocalTime;

public record DayHours(
        int dayOfWeek,
        String dayName,
        LocalTime opensAt,
        LocalTime closesAt,
        boolean closed
) {}
