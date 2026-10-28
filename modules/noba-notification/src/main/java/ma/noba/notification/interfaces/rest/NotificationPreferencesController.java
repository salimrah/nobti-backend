package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.NotificationPreferencesResponse;
import ma.noba.notification.application.dto.UpdateNotificationPreferencesRequest;
import ma.noba.notification.application.service.NotificationPreferenceService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService preferences;

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> get(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(preferences.get(require(principal)));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateNotificationPreferencesRequest request) {
        return ResponseEntity.ok(preferences.update(require(principal), request));
    }

    private static UUID require(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.id();
    }
}
