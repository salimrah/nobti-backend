package ma.noba.user;

import ma.noba.identity.domain.port.DeviceCommandPort;
import ma.noba.shared.domain.event.AccountDeletionRequested;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.user.application.dto.FcmTokenRequest;
import ma.noba.user.application.dto.UpdateNotificationPreferenceRequest;
import ma.noba.user.application.dto.UpdateProfileRequest;
import ma.noba.user.application.service.UserApplicationService;
import ma.noba.user.domain.model.NotificationPreference;
import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.model.UserProfile;
import ma.noba.user.domain.model.UserRole;
import ma.noba.user.domain.model.UserView;
import ma.noba.user.domain.port.NotificationPreferenceRepository;
import ma.noba.user.domain.port.UserProfileRepository;
import ma.noba.user.domain.port.UserRoleRepository;
import ma.noba.user.domain.port.UserServiceHistoryRepository;
import ma.noba.user.domain.port.UserViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock UserViewRepository userViewRepository;
    @Mock DeviceCommandPort deviceCommandPort;
    @Mock NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock UserServiceHistoryRepository serviceHistoryRepository;
    @Mock EventBus eventBus;

    private UserApplicationService service;
    private UUID userId;
    private UserView userView;

    @BeforeEach
    void setUp() {
        service = new UserApplicationService(userViewRepository, deviceCommandPort,
                notificationPreferenceRepository, userProfileRepository,
                userRoleRepository, serviceHistoryRepository, eventBus);
        userId = UUID.randomUUID();
        userView = new UserView(userId, "test@example.com", "+212600000000",
                UserStatus.ACTIVE, ma.noba.shared.domain.model.OnboardingStatus.COMPLETED, Instant.now());
    }

    @Test
    @DisplayName("Get user by ID — returns user response with profile and roles")
    void getUserById_success() {
        var profile = new UserProfile(userId, "John", "Doe");
        profile.setProfilePictureUrl("https://example.com/pic.jpg");
        var role = new UserRole(userId, new Role("PROVIDER", "Service provider"));

        when(userViewRepository.findById(userId)).thenReturn(Optional.of(userView));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(role));

        var result = service.getUserById(userId);

        assertThat(result.id()).isEqualTo(userId.toString());
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.profilePictureUrl()).isEqualTo("https://example.com/pic.jpg");
        assertThat(result.roles()).contains("PROVIDER");
    }

    @Test
    @DisplayName("Get user by ID — not found throws NotFoundException")
    void getUserById_notFound() {
        when(userViewRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserById(userId))
                .isInstanceOf(NotFoundException.class)
                .matches(e -> ((NotFoundException) e).getErrorCode() == ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("Update profile — existing profile gets updated")
    void updateProfile_existing() {
        var profile = new UserProfile(userId, "Old", "Name");
        var request = new UpdateProfileRequest("New", "Name", "https://pic.jpg", "fr");

        when(userViewRepository.findById(userId)).thenReturn(Optional.of(userView));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());

        var result = service.updateProfile(userId, request);

        assertThat(result.firstName()).isEqualTo("New");
        assertThat(result.lastName()).isEqualTo("Name");
        verify(userProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Update profile — no existing profile creates new one")
    void updateProfile_createsNewProfile() {
        var request = new UpdateProfileRequest("First", "Last", null, null);

        when(userViewRepository.findById(userId)).thenReturn(Optional.of(userView));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());

        var result = service.updateProfile(userId, request);

        assertThat(result.firstName()).isEqualTo("First");
        assertThat(result.lastName()).isEqualTo("Last");
        verify(userProfileRepository, times(2)).save(any(UserProfile.class));
    }

    @Test
    @DisplayName("Update profile — user not found throws exception")
    void updateProfile_userNotFound() {
        when(userViewRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(userId, new UpdateProfileRequest("F", "L", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Save FCM token — delegates to DeviceCommandPort")
    void saveFcmToken() {
        var request = new FcmTokenRequest("fcm-token-123", "ios");

        service.saveFcmToken(userId, request);

        verify(deviceCommandPort).registerDevice(userId, "fcm-token-123", "ios");
    }

    @Test
    @DisplayName("Delete FCM token — delegates to DeviceCommandPort")
    void deleteFcmToken() {
        service.deleteFcmToken(userId, "fcm-token-123");

        verify(deviceCommandPort).unregisterDevice(userId, "fcm-token-123");
    }

    @Test
    @DisplayName("Check if user is provider — returns true when has active PROVIDER role")
    void isProvider_true() {
        var role = new UserRole(userId, new Role("PROVIDER", "Service provider"));
        role.setActive(true);
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of(role));

        assertThat(service.isProvider(userId)).isTrue();
    }

    @Test
    @DisplayName("Check if user is provider — returns false when no PROVIDER role")
    void isProvider_false() {
        when(userRoleRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(service.isProvider(userId)).isFalse();
    }

    @Test
    @DisplayName("Get notification preferences — returns existing preferences")
    void getNotificationPreferences_existing() {
        var pref = new NotificationPreference(userId);
        pref.setPushEnabled(true);
        pref.setEmailEnabled(false);
        pref.setSmsEnabled(true);
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        var result = service.getNotificationPreferences(userId);

        assertThat(result.pushEnabled()).isTrue();
        assertThat(result.emailEnabled()).isFalse();
        assertThat(result.smsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Get notification preferences — returns defaults when no preferences exist")
    void getNotificationPreferences_defaults() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = service.getNotificationPreferences(userId);

        assertThat(result.pushEnabled()).isTrue();
        assertThat(result.emailEnabled()).isTrue();
        assertThat(result.smsEnabled()).isTrue();
    }

    @Test
    @DisplayName("Update notification preferences — partial update")
    void updateNotificationPreferences_partial() {
        var pref = new NotificationPreference(userId);
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        var request = new UpdateNotificationPreferenceRequest(false, null, null);

        service.updateNotificationPreferences(userId, request);

        assertThat(pref.isPushEnabled()).isFalse();
        assertThat(pref.isEmailEnabled()).isTrue();
        assertThat(pref.isSmsEnabled()).isTrue();
        verify(notificationPreferenceRepository).save(pref);
    }

    @Test
    @DisplayName("Delete account — publishes event and deletes profile")
    void deleteAccount_success() {
        when(userViewRepository.findById(userId)).thenReturn(Optional.of(userView));

        service.deleteAccount(userId);

        verify(eventBus).publish(argThat(e -> e instanceof AccountDeletionRequested r && r.getUserId().equals(userId)));
        verify(userProfileRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Delete account — user not found throws exception")
    void deleteAccount_userNotFound() {
        when(userViewRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAccount(userId))
                .isInstanceOf(NotFoundException.class);
    }
}
