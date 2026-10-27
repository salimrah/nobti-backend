package ma.noba.media.infra;

import ma.noba.media.domain.model.ProviderMedia;
import ma.noba.media.domain.port.ProviderMediaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProviderMediaRepository extends JpaRepository<ProviderMedia, UUID>, ProviderMediaRepository {

    List<ProviderMedia> findByProviderIdOrderByMediaOrderAsc(UUID providerId);
}
