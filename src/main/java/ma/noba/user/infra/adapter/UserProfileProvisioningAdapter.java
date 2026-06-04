package ma.noba.user.infra.adapter;

import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.user.domain.model.UserProfile;
import ma.noba.user.domain.port.UserProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class UserProfileProvisioningAdapter implements UserProfileProvisioningPort {

    private final UserProfileRepository userProfileRepository;

    public UserProfileProvisioningAdapter(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public void createProfile(UUID userId, String firstName, String lastName, String displayName) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(userId, firstName, lastName)));
        if (firstName != null) profile.setFirstName(firstName);
        if (lastName != null) profile.setLastName(lastName);
        if (displayName != null) profile.setDisplayName(displayName);
        userProfileRepository.save(profile);
    }
}
