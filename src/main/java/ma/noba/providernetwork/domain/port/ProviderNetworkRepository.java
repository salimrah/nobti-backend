package ma.noba.providernetwork.domain.port;

import ma.noba.providernetwork.domain.model.ProviderNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderNetworkRepository {
    ProviderNetwork save(ProviderNetwork network);
    Optional<ProviderNetwork> findById(UUID id);
    Optional<ProviderNetwork> findByProviderIdAndPartnerId(UUID providerId, UUID partnerId);
    List<ProviderNetwork> findByProviderId(UUID providerId);
    void delete(ProviderNetwork network);
}
