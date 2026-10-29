package ma.noba.media.domain.port;

import ma.noba.media.domain.model.ProviderDocument;

import java.util.List;
import java.util.UUID;

public interface ProviderDocumentRepository {

    ProviderDocument save(ProviderDocument document);

    void delete(ProviderDocument document);

    java.util.Optional<ProviderDocument> findById(UUID id);

    List<ProviderDocument> findByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<ProviderDocument> findByStatusOrderByCreatedAtAsc(String status);
}
