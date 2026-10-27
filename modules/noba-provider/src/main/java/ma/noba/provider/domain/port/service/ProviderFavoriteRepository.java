package ma.noba.provider.domain.port.service;

import ma.noba.provider.domain.model.ProviderFavorite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderFavoriteRepository {

    ProviderFavorite save(ProviderFavorite favorite);

    Optional<ProviderFavorite> findByProviderIdAndCustomerId(UUID providerId, UUID customerId);

    List<ProviderFavorite> findByCustomerId(UUID customerId);

    void deleteByProviderIdAndCustomerId(UUID providerId, UUID customerId);
}
