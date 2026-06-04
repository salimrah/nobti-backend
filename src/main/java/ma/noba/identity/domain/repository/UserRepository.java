package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.User;
import ma.noba.shared.domain.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailHash(String emailHash);
    Optional<User> findByPhoneHash(String phoneHash);
    List<User> findByStatusAndDeletedAtBefore(UserStatus status, Instant deletedAt);
    long countByStatus(UserStatus status);
}

