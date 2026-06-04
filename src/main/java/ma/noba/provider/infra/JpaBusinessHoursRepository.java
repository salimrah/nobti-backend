package ma.noba.provider.infra;

import ma.noba.provider.domain.model.BusinessHours;
import ma.noba.provider.domain.port.service.BusinessHoursRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaBusinessHoursRepository extends JpaRepository<BusinessHours, UUID>, BusinessHoursRepository {

    @Override
    default List<BusinessHours> persistAll(List<BusinessHours> hours) {
        return saveAll(hours);
    }

    List<BusinessHours> findByProviderId(UUID providerId);

    Optional<BusinessHours> findByProviderIdAndDayOfWeek(UUID providerId, int dayOfWeek);

    void deleteByProviderId(UUID providerId);
}
