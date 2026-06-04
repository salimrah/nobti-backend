package ma.noba.booking.application.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class CreateAppointmentRequest {
    @NotNull
    private String providerId;

    private String serviceId;

    @NotNull
    @Future
    private Instant startTime;

    @NotNull
    @Future
    private Instant endTime;

    private String notes;
}
