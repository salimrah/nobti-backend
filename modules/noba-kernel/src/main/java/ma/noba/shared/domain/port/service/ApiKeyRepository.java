package ma.noba.shared.domain.port.service;

import ma.noba.shared.domain.model.ApiKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {

    Optional<ApiKey> findByKeyHash(String keyHash);

    ApiKey save(ApiKey apiKey);

    Optional<ApiKey> findById(UUID id);

    List<ApiKey> findAllByOrderByCreatedAtDesc();
}
