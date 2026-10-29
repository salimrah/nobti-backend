package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.UserTotpSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTotpSecretRepository extends JpaRepository<UserTotpSecret, UUID> {
}
