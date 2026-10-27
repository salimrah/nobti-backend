package ma.noba.provider.infra;

import ma.noba.provider.domain.model.ProviderFavorite;
import ma.noba.provider.domain.port.service.ProviderFavoriteRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProviderFavoriteRepository extends JpaRepository<ProviderFavorite, UUID>, ProviderFavoriteRepository {

    Optional<ProviderFavorite> findByProviderIdAndCustomerId(UUID providerId, UUID customerId);

    List<ProviderFavorite> findByCustomerId(UUID customerId);

    void deleteByProviderIdAndCustomerId(UUID providerId, UUID customerId);
}
