package ma.noba.user.infra.adapter;

import ma.noba.identity.domain.service.UserRolePort;
import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.model.UserRole;
import ma.noba.user.domain.port.RoleRepository;
import ma.noba.user.domain.port.UserRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserRolePortAdapter implements UserRolePort {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserRolePortAdapter(UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public Set<String> findRolesByUserId(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .filter(UserRole::isActive)
                .map(r -> r.getRole().getName())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, String role) {
        Role roleEntity = roleRepository.findByName(role);
        boolean alreadyHasRole = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(r -> r.getRole().getName().equals(role) && r.isActive());
        if (!alreadyHasRole) {
            userRoleRepository.save(new UserRole(userId, roleEntity));
        }
    }
}
