package ma.noba.announcement.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.announcement.application.dto.AnnouncementResponse;
import ma.noba.announcement.application.dto.CreateAnnouncementRequest;
import ma.noba.announcement.application.service.AnnouncementApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementApplicationService announcementService;
    private final ProviderLookupPort providerLookupPort;

    @PostMapping("/api/v1/providers/self/announcements")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(announcementService.createAnnouncement(providerId, request));
    }

    @GetMapping("/api/v1/providers/{id}/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getProviderAnnouncements(@PathVariable UUID id) {
        return ResponseEntity.ok(announcementService.getProviderAnnouncements(id));
    }

    @GetMapping("/api/v1/providers/self/announcements")
    public ResponseEntity<List<AnnouncementResponse>> getMyAnnouncements(@AuthenticationPrincipal(expression = "id") UUID userId) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.ok(announcementService.getAllProviderAnnouncements(providerId));
    }

    @DeleteMapping("/api/v1/providers/self/announcements/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable UUID id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok().build();
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
