package ma.noba.employee.domain.port;

import ma.noba.employee.domain.model.ProviderEmployee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    ProviderEmployee save(ProviderEmployee employee);
    Optional<ProviderEmployee> findById(UUID id);
    List<ProviderEmployee> findByProviderId(UUID providerId);
    List<ProviderEmployee> findByProviderIdAndIsActive(UUID providerId, boolean isActive);
    void delete(ProviderEmployee employee);
}
