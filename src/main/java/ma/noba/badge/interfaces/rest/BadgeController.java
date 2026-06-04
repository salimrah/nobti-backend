package ma.noba.badge.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.badge.application.dto.BadgeResponse;
import ma.noba.badge.application.service.BadgeApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeApplicationService badgeService;

    @GetMapping("/providers/{providerId}")
    public ResponseEntity<List<BadgeResponse>> getProviderBadges(@PathVariable UUID providerId) {
        return ResponseEntity.ok(badgeService.getProviderBadges(providerId));
    }

    @PostMapping("/award")
    public ResponseEntity<BadgeResponse> awardBadge(@RequestParam UUID providerId, @RequestParam String badgeType) {
        return ResponseEntity.ok(badgeService.awardBadge(providerId, badgeType, false));
    }

    @DeleteMapping("/{badgeId}")
    public ResponseEntity<Void> removeBadge(@PathVariable UUID badgeId) {
        badgeService.removeBadge(badgeId);
        return ResponseEntity.ok().build();
    }
}
