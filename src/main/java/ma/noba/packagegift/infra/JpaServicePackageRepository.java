package ma.noba.packagegift.infra;

import ma.noba.packagegift.domain.model.ServicePackage;
import ma.noba.packagegift.domain.port.ServicePackageRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaServicePackageRepository extends JpaRepository<ServicePackage, UUID>, ServicePackageRepository {

    List<ServicePackage> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<ServicePackage> findByProviderIdAndActiveOrderByCreatedAtDesc(UUID providerId, boolean active);
}
