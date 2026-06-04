package ma.noba.providernetwork.infra;

import ma.noba.providernetwork.domain.model.ProviderNetwork;
import ma.noba.providernetwork.domain.port.ProviderNetworkRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaProviderNetworkRepository extends JpaRepository<ProviderNetwork, UUID>, ProviderNetworkRepository {

    Optional<ProviderNetwork> findByProviderIdAndPartnerId(UUID providerId, UUID partnerId);

    List<ProviderNetwork> findByProviderIdOrderByCreatedAtAsc(UUID providerId);
}
