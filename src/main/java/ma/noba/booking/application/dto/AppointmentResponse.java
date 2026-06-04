package ma.noba.booking.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AppointmentResponse {
    private String id;
    private String userId;
    private String providerId;
    private String providerName;
    private String serviceId;
    private String serviceName;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String notes;
    private Instant createdAt;
}
