package ma.noba.booking.infra;

import ma.noba.booking.domain.model.AvailabilitySlot;
import ma.noba.booking.domain.port.AvailabilitySlotRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaAvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID>, AvailabilitySlotRepository {

    List<AvailabilitySlot> findByProviderIdOrderByStartTimeAsc(UUID providerId);

    List<AvailabilitySlot> findByProviderIdAndDayOfWeek(UUID providerId, int dayOfWeek);

    List<AvailabilitySlot> findByProviderIdAndSpecificDate(UUID providerId, LocalDate specificDate);
}
