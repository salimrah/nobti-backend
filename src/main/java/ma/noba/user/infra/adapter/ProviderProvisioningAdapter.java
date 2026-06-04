package ma.noba.user.infra.adapter;

import ma.noba.provider.domain.port.service.ProviderProvisioningPort;
import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.model.UserRole;
import ma.noba.user.domain.port.UserProfileRepository;
import ma.noba.user.infra.JpaRoleRepository;
import ma.noba.user.infra.JpaUserRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ProviderProvisioningAdapter implements ProviderProvisioningPort {

    private final JpaUserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final JpaRoleRepository roleRepository;

    public ProviderProvisioningAdapter(JpaUserRoleRepository userRoleRepository,
                                       UserProfileRepository userProfileRepository,
                                       JpaRoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.userProfileRepository = userProfileRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public void onProviderCreated(UUID userId, String firstName, String lastName) {
        boolean hasProviderRole = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(r -> "PROVIDER".equals(r.getRole().getName()) && r.isActive());
        if (!hasProviderRole) {
            Role providerRole = roleRepository.findByName("PROVIDER");
            userRoleRepository.save(new UserRole(userId, providerRole));
        }

        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (firstName != null) profile.setFirstName(firstName);
            if (lastName != null) profile.setLastName(lastName);
            userProfileRepository.save(profile);
        });
    }
}
