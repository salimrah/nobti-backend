package ma.noba.user.interfaces.rest;

import ma.noba.shared.domain.user.UserPrincipal;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.user.application.dto.FcmTokenRequest;
import ma.noba.user.application.dto.NotificationPreferenceResponse;
import ma.noba.user.application.dto.ServiceHistoryResponse;
import ma.noba.user.application.dto.UpdateNotificationPreferenceRequest;
import ma.noba.user.application.dto.UpdateProfileRequest;
import ma.noba.user.application.dto.UserDataExportResponse;
import ma.noba.user.application.dto.UserResponse;
import ma.noba.user.application.service.UserApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal user) {
        UserResponse response = userService.getUserById(user.id());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(user.id(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/device")
    public ResponseEntity<Void> saveFcmToken(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody FcmTokenRequest request) {
        userService.saveFcmToken(user.id(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/device")
    public ResponseEntity<Void> deleteFcmToken(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam String fcmToken) {
        userService.deleteFcmToken(user.id(), fcmToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID userId) {
        if (!user.id().equals(userId) && !user.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal user) {
        userService.deleteAccount(user.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/onboarding-complete")
    public ResponseEntity<Void> completeOnboarding(@AuthenticationPrincipal UserPrincipal user) {
        userService.completeOnboarding(user.id());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/export")
    public ResponseEntity<UserDataExportResponse> exportData(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(userService.exportData(user.id()));
    }

    @GetMapping("/me/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> getNotificationPreferences(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(userService.getNotificationPreferences(user.id()));
    }

    @PutMapping("/me/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> updateNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(userService.updateNotificationPreferences(user.id(), request));
    }

    @GetMapping("/me/history")
    public ResponseEntity<PagedResponse<ServiceHistoryResponse>> getServiceHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getServiceHistory(user.id(), pageable));
    }
}
