package ma.noba.notification.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.NotificationResponse;
import ma.noba.notification.application.service.NotificationApplicationService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApplicationService notifications;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = require(principal);
        return ResponseEntity.ok(notifications.list(
                userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("count", notifications.unreadCount(require(principal))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return notifications.markRead(id, require(principal))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notifications.markAllRead(require(principal));
        return ResponseEntity.noContent().build();
    }

    private static UUID require(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.id();
    }
}
