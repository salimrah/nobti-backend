package ma.noba.user.infra;

import ma.noba.user.domain.model.UserRole;
import ma.noba.user.domain.port.UserRoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaUserRoleRepository extends JpaRepository<UserRole, UUID>, UserRoleRepository {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.userId = :userId")
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.role.name = :roleName")
    List<UserRole> findByRoleName(@Param("roleName") String roleName);
}
