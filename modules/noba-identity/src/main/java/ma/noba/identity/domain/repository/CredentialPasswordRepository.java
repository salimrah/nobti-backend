package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.CredentialPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredentialPasswordRepository extends JpaRepository<CredentialPassword, UUID> {
    Optional<CredentialPassword> findByUserId(UUID userId);
}
