package ma.noba.media.infra;

import ma.noba.media.domain.model.ProviderDocument;
import ma.noba.media.domain.port.ProviderDocumentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProviderDocumentRepository
        extends JpaRepository<ProviderDocument, UUID>, ProviderDocumentRepository {

    @Override
    List<ProviderDocument> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    @Override
    List<ProviderDocument> findByStatusOrderByCreatedAtAsc(String status);
}
