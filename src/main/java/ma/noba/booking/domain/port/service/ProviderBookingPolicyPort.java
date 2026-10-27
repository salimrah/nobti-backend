package ma.noba.booking.domain.port.service;

import java.util.UUID;

/** Booking policy configured per provider (with safe defaults when unset). */
public interface ProviderBookingPolicyPort {

    BookingPolicy policy(UUID providerId);

    record BookingPolicy(boolean appointmentsEnabled, int cancellationWindowMinutes) {}
}
