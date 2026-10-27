package ma.noba.booking;

import ma.noba.booking.application.service.BookingApplicationService;
import ma.noba.booking.domain.model.Appointment;
import ma.noba.booking.domain.model.AppointmentStatus;
import ma.noba.booking.domain.model.AvailabilitySlot;
import ma.noba.booking.domain.port.AppointmentRepository;
import ma.noba.booking.domain.port.AvailabilitySlotRepository;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingApplicationServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock AvailabilitySlotRepository availabilitySlotRepository;
    @Mock ProviderLookupPort providerLookupPort;
    @Mock ma.noba.booking.domain.port.service.ProviderBookingPolicyPort bookingPolicy;

    private BookingApplicationService service;

    private final UUID providerId = UUID.randomUUID();
    private final UUID otherProviderId = UUID.randomUUID();
    private final UUID ownerUserId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(bookingPolicy.policy(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ma.noba.booking.domain.port.service.ProviderBookingPolicyPort.BookingPolicy(true, 0));
        service = new BookingApplicationService(appointmentRepository, availabilitySlotRepository, providerLookupPort, bookingPolicy);
    }

    private Appointment appointment() {
        return new Appointment(UUID.randomUUID(), providerId, null,
                Instant.now(), Instant.now().plusSeconds(3600), null);
    }

    @Test
    @DisplayName("Appointment can be confirmed by its owning provider")
    void confirmByOwningProvider() {
        Appointment appointment = appointment();
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(providerLookupPort.findProviderIdByUserId(ownerUserId)).thenReturn(Optional.of(providerId));
        when(appointmentRepository.save(any())).thenReturn(appointment);

        service.confirmAppointment(id, ownerUserId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("A different provider cannot confirm the appointment (IDOR)")
    void confirmByForeignProviderIsDenied() {
        Appointment appointment = appointment();
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(providerLookupPort.findProviderIdByUserId(otherUserId)).thenReturn(Optional.of(otherProviderId));

        assertThatThrownBy(() -> service.confirmAppointment(id, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A user without a provider profile cannot complete an appointment")
    void nonProviderCannotComplete() {
        Appointment appointment = appointment();
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        lenient().when(providerLookupPort.findProviderIdByUserId(otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeAppointment(id, otherUserId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Availability slot cannot be deactivated by a foreign provider (IDOR)")
    void removeAvailabilityByForeignProviderIsDenied() {
        AvailabilitySlot slot = new AvailabilitySlot(providerId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0));
        UUID slotId = UUID.randomUUID();
        when(availabilitySlotRepository.findById(slotId)).thenReturn(Optional.of(slot));
        when(providerLookupPort.findProviderIdByUserId(otherUserId)).thenReturn(Optional.of(otherProviderId));

        assertThatThrownBy(() -> service.removeAvailabilitySlot(slotId, otherUserId))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(slot.isActive()).isTrue();
        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancellation is rejected inside the provider's cancellation window")
    void cancelWithinWindowIsRejected() {
        Appointment appointment = new Appointment(ownerUserId, providerId, null,
                Instant.now().plusSeconds(600), Instant.now().plusSeconds(2400), null);
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(bookingPolicy.policy(providerId))
                .thenReturn(new ma.noba.booking.domain.port.service.ProviderBookingPolicyPort.BookingPolicy(true, 60));

        assertThatThrownBy(() -> service.cancelAppointment(id, ownerUserId))
                .isInstanceOf(ma.noba.shared.domain.exception.ValidationException.class);
        verify(appointmentRepository, never()).save(any());
    }
}
