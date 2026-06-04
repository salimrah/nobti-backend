package ma.noba.provider.infra;

import ma.noba.provider.domain.model.ServiceCategory;
import ma.noba.provider.domain.port.service.ServiceCategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID>, ServiceCategoryRepository {

    Optional<ServiceCategory> findByName(String name);
}
