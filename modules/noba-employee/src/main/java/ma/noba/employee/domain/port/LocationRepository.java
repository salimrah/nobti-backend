package ma.noba.employee.domain.port;

import ma.noba.employee.domain.model.ProviderLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository {
    ProviderLocation save(ProviderLocation location);
    Optional<ProviderLocation> findById(UUID id);
    List<ProviderLocation> findByProviderId(UUID providerId);
    List<ProviderLocation> findByProviderIdAndIsActive(UUID providerId, boolean isActive);
    void delete(ProviderLocation location);
}
