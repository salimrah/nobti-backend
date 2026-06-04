package ma.noba.media.domain.port;

import ma.noba.media.domain.model.ProviderMedia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderMediaRepository {
    ProviderMedia save(ProviderMedia media);
    Optional<ProviderMedia> findById(UUID id);
    List<ProviderMedia> findByProviderIdOrderByMediaOrderAsc(UUID providerId);
    void delete(ProviderMedia media);
}
