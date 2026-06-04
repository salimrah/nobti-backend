package ma.noba.booking.domain.port;

import ma.noba.booking.domain.model.Appointment;
import ma.noba.booking.domain.model.AppointmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(UUID id);
    List<Appointment> findByUserId(UUID userId);
    List<Appointment> findByProviderIdAndDateRange(UUID providerId, Instant start, Instant end);
    List<Appointment> findByUserIdAndStatus(UUID userId, AppointmentStatus status);
    List<Appointment> findByProviderIdAndStatus(UUID providerId, AppointmentStatus status);
    List<Appointment> findByProviderId(UUID providerId);
    boolean existsOverlapping(UUID providerId, Instant startTime, Instant endTime, UUID excludeId);
}
