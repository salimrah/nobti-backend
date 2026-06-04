package ma.noba.shared.infra.jpa;

import ma.noba.shared.domain.model.ApiKey;
import ma.noba.shared.domain.port.service.ApiKeyRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaApiKeyRepository extends JpaRepository<ApiKey, UUID>, ApiKeyRepository {

    Optional<ApiKey> findByKeyHash(String keyHash);
}
