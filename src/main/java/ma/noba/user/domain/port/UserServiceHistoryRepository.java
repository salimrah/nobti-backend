package ma.noba.user.domain.port;

import ma.noba.user.domain.model.UserServiceHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserServiceHistoryRepository {
    UserServiceHistory save(UserServiceHistory history);
    Page<UserServiceHistory> findByUserIdOrderByCompletedAtDesc(UUID userId, Pageable pageable);
}
