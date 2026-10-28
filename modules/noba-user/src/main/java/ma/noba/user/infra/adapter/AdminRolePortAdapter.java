package ma.noba.user.infra.adapter;

import ma.noba.shared.domain.port.AdminRolePort;
import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.model.UserRole;
import ma.noba.user.domain.port.RoleRepository;
import ma.noba.user.domain.port.UserRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class AdminRolePortAdapter implements AdminRolePort {

    private static final Set<String> ASSIGNABLE = new LinkedHashSet<>(
            List.of("USER", "PROVIDER", "DELIVERER", "ADMIN"));

    private final UserRoleRepository userRoles;
    private final RoleRepository roleRepository;

    public AdminRolePortAdapter(UserRoleRepository userRoles, RoleRepository roleRepository) {
        this.userRoles = userRoles;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> rolesOf(UUID userId) {
        return userRoles.findByUserId(userId).stream()
                .filter(UserRole::isActive)
                .map(userRole -> userRole.getRole().getName())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional
    public void replaceRoles(UUID userId, Set<String> roles) {
        Set<String> wanted = new HashSet<>();
        for (String role : roles) {
            wanted.add(role.trim().toUpperCase(Locale.ROOT));
        }
        wanted.retainAll(ASSIGNABLE);

        List<UserRole> existing = userRoles.findByUserId(userId);
        for (UserRole userRole : existing) {
            if (!wanted.contains(userRole.getRole().getName())) {
                userRoles.remove(userRole);
            } else {
                wanted.remove(userRole.getRole().getName());
            }
        }
        for (String name : wanted) {
            Role role = roleRepository.findByName(name);
            if (role == null) {
                throw new IllegalArgumentException("Unknown role: " + name);
            }
            userRoles.save(new UserRole(userId, role));
        }
    }

    @Override
    public long countAdmins() {
        return userRoles.countByRoleName("ADMIN");
    }

    @Override
    public Set<String> assignableRoles() {
        return Set.copyOf(ASSIGNABLE);
    }
}
