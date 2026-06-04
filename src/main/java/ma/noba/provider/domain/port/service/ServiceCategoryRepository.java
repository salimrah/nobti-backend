package ma.noba.provider.domain.port.service;

import ma.noba.provider.domain.model.ServiceCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCategoryRepository {

    List<ServiceCategory> findAll();

    Optional<ServiceCategory> findById(UUID id);

    Optional<ServiceCategory> findByName(String name);

    ServiceCategory save(ServiceCategory category);
}
