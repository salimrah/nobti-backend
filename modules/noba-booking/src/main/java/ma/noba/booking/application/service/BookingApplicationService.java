package ma.noba.booking.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.booking.application.dto.*;
import ma.noba.booking.domain.model.Appointment;
import ma.noba.booking.domain.model.AppointmentStatus;
import ma.noba.booking.domain.model.AvailabilitySlot;
import ma.noba.booking.domain.port.AppointmentRepository;
import ma.noba.booking.domain.port.AvailabilitySlotRepository;
import ma.noba.booking.domain.port.service.ProviderBookingPolicyPort;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingApplicationService {

    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ProviderLookupPort providerLookupPort;
    private final ProviderBookingPolicyPort bookingPolicy;

    public AvailableSlotsResponse getAvailableSlots(String providerId, LocalDate date) {
        UUID pid = UUID.fromString(providerId);
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;

        List<AvailabilitySlot> recurring = availabilitySlotRepository.findByProviderIdAndDayOfWeek(pid, dayOfWeek);
        List<AvailabilitySlot> specific = availabilitySlotRepository.findByProviderIdAndSpecificDate(pid, date);

        Set<UUID> seen = new HashSet<>();
        List<AvailabilitySlot> allSlots = new ArrayList<>();
        for (AvailabilitySlot s : specific) {
            if (seen.add(s.getId())) allSlots.add(s);
        }
        for (AvailabilitySlot s : recurring) {
            if (seen.add(s.getId())) allSlots.add(s);
        }

        Instant dayStart = date.atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        List<Appointment> existing = appointmentRepository.findByProviderIdAndDateRange(pid, dayStart, dayEnd);

        List<AvailableSlotsResponse.TimeSlot> timeSlots = new ArrayList<>();
        for (AvailabilitySlot slot : allSlots) {
            if (!slot.isActive()) continue;

            LocalTime current = slot.getStartTime();
            while (current.isBefore(slot.getEndTime())) {
                LocalTime slotEnd = current.plusMinutes(30);
                if (slotEnd.isAfter(slot.getEndTime())) break;

                Instant slotStartInst = date.atTime(current).atZone(ZoneId.of("UTC")).toInstant();
                Instant slotEndInst = date.atTime(slotEnd).atZone(ZoneId.of("UTC")).toInstant();

                boolean isBooked = existing.stream().anyMatch(a ->
                    a.getStatus() != AppointmentStatus.CANCELLED &&
                    a.getStartTime().isBefore(slotEndInst) &&
                    a.getEndTime().isAfter(slotStartInst)
                );
                // Slots that already started today must not be offered for booking.
                boolean isPast = slotStartInst.isBefore(Instant.now());

                timeSlots.add(new AvailableSlotsResponse.TimeSlot(current, slotEnd, !isBooked && !isPast));
                current = slotEnd;
            }
        }

        return new AvailableSlotsResponse(date, dayOfWeek, timeSlots);
    }

    @Transactional
    public AppointmentResponse bookAppointment(UUID userId, CreateAppointmentRequest request) {
        UUID providerId = UUID.fromString(request.getProviderId());

        if (!bookingPolicy.policy(providerId).appointmentsEnabled()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "Appointments are currently disabled for this provider");
        }

        if (appointmentRepository.existsOverlapping(providerId, request.getStartTime(), request.getEndTime(), null)) {
            throw new IllegalStateException("Time slot is already booked");
        }

        UUID serviceId = request.getServiceId() != null ? UUID.fromString(request.getServiceId()) : null;
        Appointment appointment = new Appointment(
            userId, providerId, serviceId,
            request.getStartTime(), request.getEndTime(), request.getNotes()
        );
        appointment.confirm();
        appointment = appointmentRepository.save(appointment);

        return toResponse(appointment, null, null);
    }

    @Transactional
    public void cancelAppointment(UUID appointmentId, UUID userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        if (!appointment.getUserId().equals(userId)) {
            throw new AccessDeniedException("Not authorized to cancel this appointment");
        }

        int windowMinutes = bookingPolicy.policy(appointment.getProviderId()).cancellationWindowMinutes();
        if (windowMinutes > 0 && appointment.getStartTime() != null
                && Instant.now().isAfter(appointment.getStartTime().minus(Duration.ofMinutes(windowMinutes)))) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "The cancellation window for this appointment has passed");
        }

        appointment.cancel();
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void confirmAppointment(UUID appointmentId, UUID userId) {
        Appointment appointment = findAppointment(appointmentId);
        requireProviderOwns(appointment.getProviderId(), userId);
        appointment.confirm();
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void completeAppointment(UUID appointmentId, UUID userId) {
        Appointment appointment = findAppointment(appointmentId);
        requireProviderOwns(appointment.getProviderId(), userId);
        appointment.complete();
        appointmentRepository.save(appointment);
    }

    @Transactional
    public void markNoShow(UUID appointmentId, UUID userId) {
        Appointment appointment = findAppointment(appointmentId);
        requireProviderOwns(appointment.getProviderId(), userId);
        appointment.markNoShow();
        appointmentRepository.save(appointment);
    }

    private Appointment findAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    /** Ensures the caller is the provider that owns the given resource. */
    private void requireProviderOwns(UUID resourceProviderId, UUID userId) {
        UUID callerProviderId = providerLookupPort.findProviderIdByUserId(userId).orElse(null);
        if (callerProviderId == null || !callerProviderId.equals(resourceProviderId)) {
            throw new AccessDeniedException("Not the provider of this resource");
        }
    }

    public List<Appointment> getUserAppointments(UUID userId) {
        return appointmentRepository.findByUserId(userId);
    }

    public List<Appointment> getProviderAppointments(UUID providerId, LocalDate date) {
        if (date != null) {
            Instant start = date.atStartOfDay(ZoneId.of("UTC")).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
            return appointmentRepository.findByProviderIdAndDateRange(providerId, start, end);
        }
        return appointmentRepository.findByProviderId(providerId);
    }

    public List<AvailabilitySlotResponse> getAvailabilitySlots(String providerId) {
        UUID pid = UUID.fromString(providerId);
        return availabilitySlotRepository.findByProviderId(pid).stream()
            .map(s -> new AvailabilitySlotResponse(
                s.getId().toString(), s.getProviderId().toString(),
                s.getDayOfWeek(), s.getSpecificDate(),
                s.getStartTime(), s.getEndTime(), s.isActive()
            ))
            .toList();
    }

    @Transactional
    public AvailabilitySlotResponse addAvailabilitySlot(CreateAvailabilitySlotRequest request) {
        UUID providerId = UUID.fromString(request.getProviderId());
        AvailabilitySlot slot;
        if (request.getSpecificDate() != null) {
            slot = new AvailabilitySlot(providerId, request.getSpecificDate(), request.getStartTime(), request.getEndTime());
        } else {
            slot = new AvailabilitySlot(providerId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime());
        }
        slot = availabilitySlotRepository.save(slot);
        return new AvailabilitySlotResponse(
            slot.getId().toString(), slot.getProviderId().toString(),
            slot.getDayOfWeek(), slot.getSpecificDate(),
            slot.getStartTime(), slot.getEndTime(), slot.isActive()
        );
    }

    @Transactional
    public void removeAvailabilitySlot(UUID slotId, UUID userId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Availability slot not found: " + slotId));
        requireProviderOwns(slot.getProviderId(), userId);
        slot.deactivate();
        availabilitySlotRepository.save(slot);
    }

    public static AppointmentResponse toResponse(Appointment a, String providerName, String serviceName) {
        return new AppointmentResponse(
            a.getId().toString(), a.getUserId().toString(), a.getProviderId().toString(),
            providerName,
            a.getServiceId() != null ? a.getServiceId().toString() : null,
            serviceName,
            a.getStartTime(), a.getEndTime(),
            a.getStatus().name(), a.getNotes(), a.getCreatedAt()
        );
    }
}
