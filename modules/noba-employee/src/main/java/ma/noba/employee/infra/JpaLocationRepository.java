package ma.noba.employee.infra;

import ma.noba.employee.domain.model.ProviderLocation;
import ma.noba.employee.domain.port.LocationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLocationRepository extends JpaRepository<ProviderLocation, UUID>, LocationRepository {
    List<ProviderLocation> findByProviderIdOrderByNameAsc(UUID providerId);
    List<ProviderLocation> findByProviderIdAndIsActiveOrderByNameAsc(UUID providerId, boolean isActive);
}
