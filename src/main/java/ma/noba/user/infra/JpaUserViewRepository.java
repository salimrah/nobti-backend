package ma.noba.user.infra;

import ma.noba.user.domain.model.UserView;
import ma.noba.user.domain.port.UserViewRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaUserViewRepository extends JpaRepository<UserView, UUID>, UserViewRepository {
}
