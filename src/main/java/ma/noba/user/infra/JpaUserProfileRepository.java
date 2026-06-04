package ma.noba.user.infra;

import ma.noba.user.domain.model.UserProfile;
import ma.noba.user.domain.port.UserProfileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfile, UUID>, UserProfileRepository {

    Optional<UserProfile> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
