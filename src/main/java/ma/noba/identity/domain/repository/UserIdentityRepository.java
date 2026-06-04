package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByProviderTypeAndIdentifierHash(String providerType, String identifierHash);
    List<UserIdentity> findByUserId(UUID userId);
    List<UserIdentity> findByUserIdAndIsPrimaryTrue(UUID userId);
}
