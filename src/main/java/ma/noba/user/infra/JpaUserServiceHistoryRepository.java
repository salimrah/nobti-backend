package ma.noba.user.infra;

import ma.noba.user.domain.model.UserServiceHistory;
import ma.noba.user.domain.port.UserServiceHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserServiceHistoryRepository extends JpaRepository<UserServiceHistory, UUID>, UserServiceHistoryRepository {
    Page<UserServiceHistory> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);
}
