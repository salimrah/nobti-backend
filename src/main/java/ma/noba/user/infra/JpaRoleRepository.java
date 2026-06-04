package ma.noba.user.infra;

import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.port.RoleRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaRoleRepository extends JpaRepository<Role, UUID>, RoleRepository {

    Role findByName(String name);
}
