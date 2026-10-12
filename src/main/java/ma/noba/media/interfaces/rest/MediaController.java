package ma.noba.media.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.media.application.dto.AddMediaRequest;
import ma.noba.media.application.dto.AddProviderMediaRequest;
import ma.noba.media.application.dto.ProviderMediaResponse;
import ma.noba.media.application.dto.ReviewMediaResponse;
import ma.noba.media.application.service.MediaApplicationService;
import ma.noba.payment.domain.port.ProviderLookupPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MediaController {

    private final MediaApplicationService mediaService;
    private final ProviderLookupPort providerLookupPort;

    @GetMapping("/reviews/{reviewId}/media")
    public ResponseEntity<List<ReviewMediaResponse>> getReviewMedia(@PathVariable UUID reviewId) {
        return ResponseEntity.ok(mediaService.getReviewMedia(reviewId));
    }

    @PostMapping("/reviews/{reviewId}/media")
    public ResponseEntity<ReviewMediaResponse> addMedia(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @PathVariable UUID reviewId,
        @Valid @RequestBody AddMediaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mediaService.addMedia(reviewId, request));
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<Void> removeMedia(@PathVariable UUID mediaId) {
        mediaService.removeMedia(mediaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers/{id}/media")
    public ResponseEntity<List<ProviderMediaResponse>> getProviderMedia(@PathVariable UUID id) {
        return ResponseEntity.ok(mediaService.getProviderMedia(id));
    }

    @PostMapping("/providers/self/media")
    public ResponseEntity<ProviderMediaResponse> addProviderMedia(
        @AuthenticationPrincipal(expression = "id") UUID userId,
        @Valid @RequestBody AddProviderMediaRequest request
    ) {
        UUID providerId = getProviderId(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mediaService.addProviderMedia(providerId, request));
    }

    @DeleteMapping("/providers/self/media/{mediaId}")
    public ResponseEntity<Void> removeProviderMedia(@PathVariable UUID mediaId) {
        mediaService.removeProviderMedia(mediaId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/providers/self/media/{mediaId}/order")
    public ResponseEntity<ProviderMediaResponse> updateProviderMediaOrder(
        @PathVariable UUID mediaId,
        @RequestBody Map<String, Integer> body
    ) {
        return ResponseEntity.ok(mediaService.updateProviderMediaOrder(mediaId, body.get("mediaOrder")));
    }

    private UUID getProviderId(UUID userId) {
        return providerLookupPort.findProviderIdByUserId(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No provider profile found"));
    }
}
