package ma.noba.user.infra.adapter;

import ma.noba.shared.domain.port.AdminUserPort;
import ma.noba.user.domain.port.UserRoleRepository;
import org.springframework.stereotype.Component;

@Component
public class AdminUserPortAdapter implements AdminUserPort {

    private final UserRoleRepository userRoleRepository;

    public AdminUserPortAdapter(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public long countProviders() {
        return userRoleRepository.countByRoleName("PROVIDER");
    }
}
