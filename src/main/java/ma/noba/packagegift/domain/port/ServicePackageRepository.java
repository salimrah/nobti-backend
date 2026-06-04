package ma.noba.packagegift.domain.port;

import ma.noba.packagegift.domain.model.ServicePackage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicePackageRepository {
    ServicePackage save(ServicePackage servicePackage);
    Optional<ServicePackage> findById(UUID id);
    List<ServicePackage> findByProviderId(UUID providerId);
    List<ServicePackage> findByProviderIdAndActive(UUID providerId, boolean active);
    void delete(ServicePackage servicePackage);
}
