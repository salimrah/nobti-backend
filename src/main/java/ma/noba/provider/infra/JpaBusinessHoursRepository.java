package ma.noba.provider.infra;

import ma.noba.provider.domain.model.BusinessHours;
import ma.noba.provider.domain.port.service.BusinessHoursRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Bulk delete executed immediately. A Spring Data derived delete would only be applied at
     * flush time, and Hibernate flushes inserts before deletes, so re-saving business hours for a
     * provider that already had rows violated {@code uq_business_hours_provider_day}.
     */
    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BusinessHours b WHERE b.providerId = :providerId")
    void deleteByProviderId(@Param("providerId") UUID providerId);
}
