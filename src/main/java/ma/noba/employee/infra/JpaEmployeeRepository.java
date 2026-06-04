package ma.noba.employee.infra;

import ma.noba.employee.domain.model.ProviderEmployee;
import ma.noba.employee.domain.port.EmployeeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaEmployeeRepository extends JpaRepository<ProviderEmployee, UUID>, EmployeeRepository {

    List<ProviderEmployee> findByProviderIdOrderByNameAsc(UUID providerId);

    List<ProviderEmployee> findByProviderIdAndIsActiveOrderByNameAsc(UUID providerId, boolean isActive);
}
