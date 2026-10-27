package ma.noba.booking.infra;

import ma.noba.booking.domain.model.Appointment;
import ma.noba.booking.domain.model.AppointmentStatus;
import ma.noba.booking.domain.port.AppointmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<Appointment, UUID>, AppointmentRepository {

    List<Appointment> findByUserIdOrderByStartTimeAsc(UUID userId);

    @Query("SELECT a FROM Appointment a WHERE a.providerId = :pid AND a.startTime >= :start AND a.startTime < :end ORDER BY a.startTime")
    List<Appointment> findByProviderIdAndDateRange(@Param("pid") UUID providerId, @Param("start") Instant start, @Param("end") Instant end);

    List<Appointment> findByUserIdAndStatus(UUID userId, AppointmentStatus status);

    List<Appointment> findByProviderIdAndStatus(UUID providerId, AppointmentStatus status);

    List<Appointment> findByProviderIdOrderByStartTimeAsc(UUID providerId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.providerId = :pid AND a.status <> 'CANCELLED' AND a.startTime < :endTime AND a.endTime > :startTime AND (:excludeId IS NULL OR a.id <> :excludeId)")
    boolean existsOverlapping(@Param("pid") UUID providerId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime, @Param("excludeId") UUID excludeId);
}
