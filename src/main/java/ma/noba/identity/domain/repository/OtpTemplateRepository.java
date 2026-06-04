package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.OtpTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpTemplateRepository extends JpaRepository<OtpTemplate, UUID> {
    Optional<OtpTemplate> findByLocaleAndName(String locale, String name);
}
