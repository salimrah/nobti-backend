package ma.noba.qa.infra;

import ma.noba.qa.domain.model.ProviderQuestion;
import ma.noba.qa.domain.port.ProviderQuestionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaProviderQuestionRepository extends JpaRepository<ProviderQuestion, UUID>, ProviderQuestionRepository {
    List<ProviderQuestion> findByProviderIdOrderByCreatedAtDesc(UUID providerId);
    List<ProviderQuestion> findByUserId(UUID userId);
}
