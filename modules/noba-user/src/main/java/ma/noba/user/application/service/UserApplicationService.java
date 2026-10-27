package ma.noba.user.application.service;

import lombok.extern.slf4j.Slf4j;
import ma.noba.identity.domain.port.DeviceCommandPort;
import ma.noba.shared.domain.event.AccountDeletionRequested;
import ma.noba.shared.domain.event.EventBus;
import ma.noba.shared.domain.event.UserOnboardingCompletedEvent;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.user.application.dto.FcmTokenRequest;
import ma.noba.user.application.dto.NotificationPreferenceResponse;
import ma.noba.user.application.dto.ServiceHistoryResponse;
import ma.noba.user.application.dto.UpdateNotificationPreferenceRequest;
import ma.noba.user.application.dto.UpdateProfileRequest;
import ma.noba.user.application.dto.UserDataExportResponse;
import ma.noba.user.application.dto.UserResponse;
import ma.noba.user.domain.model.NotificationPreference;
import ma.noba.user.domain.model.UserProfile;
import ma.noba.user.domain.model.UserServiceHistory;
import ma.noba.user.domain.model.UserView;
import ma.noba.user.domain.port.NotificationPreferenceRepository;
import ma.noba.user.domain.port.UserProfileRepository;
import ma.noba.user.domain.port.UserRoleRepository;
import ma.noba.user.domain.port.UserServiceHistoryRepository;
import ma.noba.user.domain.port.UserViewRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserApplicationService {

    private final UserViewRepository userViewRepository;
    private final DeviceCommandPort deviceCommandPort;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserServiceHistoryRepository serviceHistoryRepository;
    private final EventBus eventBus;

    public UserApplicationService(UserViewRepository userViewRepository, DeviceCommandPort deviceCommandPort,
                                  NotificationPreferenceRepository notificationPreferenceRepository,
                                  UserProfileRepository userProfileRepository,
                                  UserRoleRepository userRoleRepository,
                                  UserServiceHistoryRepository serviceHistoryRepository,
                                  EventBus eventBus) {
        this.userViewRepository = userViewRepository;
        this.deviceCommandPort = deviceCommandPort;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userProfileRepository = userProfileRepository;
        this.userRoleRepository = userRoleRepository;
        this.serviceHistoryRepository = serviceHistoryRepository;
        this.eventBus = eventBus;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user by id: {}", userId);
        UserView user = userViewRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
                });

        var profile = userProfileRepository.findByUserId(userId).orElse(null);
        var roles = userRoleRepository.findByUserId(userId).stream()
                .map(r -> r.getRole().getName())
                .collect(Collectors.toSet());
        log.debug("Returning user: {}", userId);
        return new UserResponse(
                user.getId().toString(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getProfilePictureUrl() : null,
                roles
        );
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);
        userViewRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for profile update: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
                });

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(userId, request.firstName(), request.lastName())));

        profile.update(request.firstName(), request.lastName(), request.profilePictureUrl(), request.preferredLanguage());
        userProfileRepository.save(profile);

        var roles = userRoleRepository.findByUserId(userId).stream()
                .map(r -> r.getRole().getName())
                .collect(Collectors.toSet());
        return new UserResponse(
                userId.toString(), null, null,
                UserStatus.ACTIVE,
                profile.getFirstName(), profile.getLastName(), profile.getProfilePictureUrl(),
                roles
        );
    }

    @Transactional
    public void saveFcmToken(UUID userId, FcmTokenRequest request) {
        log.info("Saving FCM token for user: {}", userId);
        deviceCommandPort.registerDevice(userId, request.fcmToken(), request.platform());
    }

    @Transactional
    public void deleteFcmToken(UUID userId, String fcmToken) {
        log.info("Deleting FCM token for user: {}", userId);
        deviceCommandPort.unregisterDevice(userId, fcmToken);
    }

    @Transactional(readOnly = true)
    public boolean isProvider(UUID userId) {
        boolean result = userRoleRepository.findByUserId(userId).stream()
                .anyMatch(r -> "PROVIDER".equals(r.getRole().getName()) && r.isActive());
        log.debug("isProvider check for user {}: {}", userId, result);
        return result;
    }

    @Transactional
    public void completeOnboarding(UUID userId) {
        log.info("Completing onboarding for user: {}", userId);
        UserView user = userViewRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for onboarding: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
                });
        user.setOnboardingStatus(OnboardingStatus.COMPLETED);
        userViewRepository.save(user);
        eventBus.publish(new UserOnboardingCompletedEvent(userId));
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getNotificationPreferences(UUID userId) {
        log.debug("Getting notification preferences for user: {}", userId);
        var pref = notificationPreferenceRepository.findByUserId(userId)
                .orElse(new NotificationPreference(userId));
        return new NotificationPreferenceResponse(pref.isPushEnabled(), pref.isEmailEnabled(), pref.isSmsEnabled());
    }

    @Transactional
    public NotificationPreferenceResponse updateNotificationPreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
        log.info("Updating notification preferences for user: {}", userId);
        var pref = notificationPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> notificationPreferenceRepository.save(new NotificationPreference(userId)));
        if (request.pushEnabled() != null) pref.setPushEnabled(request.pushEnabled());
        if (request.emailEnabled() != null) pref.setEmailEnabled(request.emailEnabled());
        if (request.smsEnabled() != null) pref.setSmsEnabled(request.smsEnabled());
        notificationPreferenceRepository.save(pref);
        return new NotificationPreferenceResponse(pref.isPushEnabled(), pref.isEmailEnabled(), pref.isSmsEnabled());
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        log.warn("Deleting account for user: {}", userId);
        userViewRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for deletion: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
                });
        eventBus.publish(new AccountDeletionRequested(userId));
        userProfileRepository.deleteByUserId(userId);
        log.info("Account deleted for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ServiceHistoryResponse> getServiceHistory(UUID userId, Pageable pageable) {
        log.debug("Getting service history for user: {}", userId);
        var page = serviceHistoryRepository.findByUserIdOrderByCompletedAtDesc(userId, pageable)
                .map(h -> new ServiceHistoryResponse(
                        h.getId().toString(),
                        h.getUserId().toString(),
                        h.getProviderId().toString(),
                        h.getProviderName(),
                        h.getServiceId(),
                        h.getServiceName(),
                        h.getCompletedAt(),
                        h.getPrice(),
                        h.getRating()
                ));
        return PagedResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserDataExportResponse exportData(UUID userId) {
        log.info("Exporting data for user: {}", userId);
        UserView user = userViewRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for data export: {}", userId);
                    return new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found");
                });
        var profile = userProfileRepository.findByUserId(userId).orElse(null);
        var roles = userRoleRepository.findByUserId(userId).stream()
                .map(r -> r.getRole().getName())
                .collect(Collectors.toSet());
        return new UserDataExportResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getProfilePictureUrl() : null,
                roles,
                user.getCreatedAt()
        );
    }
}
