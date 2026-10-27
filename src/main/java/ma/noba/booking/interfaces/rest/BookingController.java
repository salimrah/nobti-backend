package ma.noba.booking.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.booking.application.dto.*;
import ma.noba.booking.application.service.BookingApplicationService;
import ma.noba.booking.domain.model.Appointment;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingApplicationService bookingService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping("/providers/{providerId}/slots")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
        @PathVariable String providerId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(bookingService.getAvailableSlots(providerId, date));
    }

    @PostMapping("/providers/{providerId}/appointments")
    public ResponseEntity<AppointmentResponse> bookAppointment(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable String providerId,
        @Valid @RequestBody CreateAppointmentRequest request
    ) {
        if (!request.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider ID mismatch");
        }
        AppointmentResponse response = bookingService.bookAppointment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/appointments/me")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(@AuthenticationPrincipal(expression = "id") UUID userId) {
        List<Appointment> appointments = bookingService.getUserAppointments(userId);
        List<AppointmentResponse> response = appointments.stream()
            .map(a -> BookingApplicationService.toResponse(a, null, null))
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/self/appointments")
    public ResponseEntity<List<AppointmentResponse>> getMyProviderAppointments(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        UUID providerId = getProviderIdForUser(userId);
        List<Appointment> appointments = bookingService.getProviderAppointments(providerId, date);
        List<AppointmentResponse> response = appointments.stream()
            .map(a -> BookingApplicationService.toResponse(a, null, null))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/appointments/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID id
    ) {
        bookingService.cancelAppointment(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/appointments/{id}/confirm")
    public ResponseEntity<Void> confirmAppointment(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID id
    ) {
        bookingService.confirmAppointment(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/appointments/{id}/complete")
    public ResponseEntity<Void> completeAppointment(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID id
    ) {
        bookingService.completeAppointment(id, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/appointments/{id}/no-show")
    public ResponseEntity<Void> markNoShow(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID id
    ) {
        bookingService.markNoShow(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/self/availability")
    public ResponseEntity<List<AvailabilitySlotResponse>> getMyAvailability(
        @AuthenticationPrincipal(expression = "id") UUID userId
    ) {
        UUID providerId = getProviderIdForUser(userId);
        return ResponseEntity.ok(bookingService.getAvailabilitySlots(providerId.toString()));
    }

    @PostMapping("/providers/self/availability")
    public ResponseEntity<AvailabilitySlotResponse> addAvailability(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody CreateAvailabilitySlotRequest request
    ) {
        UUID providerId = getProviderIdForUser(userId);
        request.setProviderId(providerId.toString());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(bookingService.addAvailabilitySlot(request));
    }

    @DeleteMapping("/providers/self/availability/{slotId}")
    public ResponseEntity<Void> removeAvailability(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID slotId
    ) {
        bookingService.removeAvailabilitySlot(slotId, userId);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderIdForUser(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
