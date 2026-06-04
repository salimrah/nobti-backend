package ma.noba.shared.domain.port.service;

import ma.noba.shared.domain.model.ApiKey;

import java.util.Optional;

public interface ApiKeyRepository {

    Optional<ApiKey> findByKeyHash(String keyHash);

    ApiKey save(ApiKey apiKey);
}
