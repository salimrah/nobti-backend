package ma.noba.identity.domain.repository;

import ma.noba.identity.domain.model.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {
}
