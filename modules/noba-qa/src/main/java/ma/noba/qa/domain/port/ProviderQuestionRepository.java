package ma.noba.qa.domain.port;

import ma.noba.qa.domain.model.ProviderQuestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderQuestionRepository {
    ProviderQuestion save(ProviderQuestion question);
    Optional<ProviderQuestion> findById(UUID id);
    List<ProviderQuestion> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
    List<ProviderQuestion> findByUserId(UUID userId);
}
