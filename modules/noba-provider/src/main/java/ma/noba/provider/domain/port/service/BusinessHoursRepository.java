package ma.noba.provider.domain.port.service;

import ma.noba.provider.domain.model.BusinessHours;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessHoursRepository {

    List<BusinessHours> persistAll(List<BusinessHours> hours);

    List<BusinessHours> findByProviderId(UUID providerId);

    Optional<BusinessHours> findByProviderIdAndDayOfWeek(UUID providerId, int dayOfWeek);

    void deleteByProviderId(UUID providerId);
}
