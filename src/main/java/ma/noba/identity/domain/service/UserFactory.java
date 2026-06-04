package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.UserProfileProvisioningPort;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.shared.DomainService;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserSignedUpEvent;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;

import java.util.UUID;

@DomainService
@RequiredArgsConstructor
public class UserFactory {

    private final UserRepository userRepository;
    private final UserProfileProvisioningPort userProfileProvisioning;
    private final EventBus eventBus;
    private final UserRolePort userRolePort;

    public User createWithEmail(String email, String emailHash) {
        var user = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .emailHash(emailHash)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build());
        userProfileProvisioning.createProfile(user.getId(), null, null, null);
        userRolePort.assignRole(user.getId(), "USER");
        eventBus.publish(new UserSignedUpEvent(
                user.getId().toString(), email, null, UserStatus.ACTIVE, user.getOnboardingStatus()));
        return user;
    }

    public User createWithPhone(String phone, String phoneHash) {
        var user = userRepository.save(User.builder()
                .id(UUID.randomUUID())
                .phone(phone)
                .phoneHash(phoneHash)
                .phoneVerified(true)
                .status(UserStatus.ACTIVE)
                .build());
        userProfileProvisioning.createProfile(user.getId(), null, null, null);
        userRolePort.assignRole(user.getId(), "USER");
        eventBus.publish(new UserSignedUpEvent(
                user.getId().toString(), null, phone, UserStatus.ACTIVE, user.getOnboardingStatus()));
        return user;
    }
}
