package ma.noba.booking.domain.port;

import ma.noba.booking.domain.model.AvailabilitySlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilitySlotRepository {
    AvailabilitySlot save(AvailabilitySlot slot);
    Optional<AvailabilitySlot> findById(UUID id);
    List<AvailabilitySlot> findByProviderId(UUID providerId);
    List<AvailabilitySlot> findByProviderIdAndDayOfWeek(UUID providerId, int dayOfWeek);
    List<AvailabilitySlot> findByProviderIdAndSpecificDate(UUID providerId, LocalDate date);
    void delete(AvailabilitySlot slot);
}
