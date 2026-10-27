package ma.noba.queue.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.queue.application.dto.WaitlistEntryResponse;
import ma.noba.queue.application.service.QueueWaitlistApplicationService;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Client + provider endpoints for the persistent waitlist. */
@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueWaitlistController {

    private final QueueWaitlistApplicationService waitlistService;
    private final ProviderLookupPort providerLookupPort;

    /** A client registers interest in a provider's queue (e.g. while it is closed). */
    @PostMapping("/providers/{providerId}/waitlist")
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        WaitlistEntryResponse response = waitlistService.joinWaitlist(providerId, user.id());
        return ResponseEntity.created(URI.create("/api/v1/queues/waitlist/" + response.id())).body(response);
    }

    @DeleteMapping("/providers/{providerId}/waitlist")
    public ResponseEntity<Void> leaveWaitlist(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        waitlistService.leaveWaitlist(providerId, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/waitlist/me")
    public ResponseEntity<List<WaitlistEntryResponse>> getMyWaitlist(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(waitlistService.getMyWaitlist(user.id()));
    }

    @GetMapping("/providers/self/waitlist")
    public ResponseEntity<List<WaitlistEntryResponse>> getProviderWaitlist(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(waitlistService.getProviderWaitlist(requireProviderId(user)));
    }

    private UUID requireProviderId(UserPrincipal user) {
        if (user == null || !user.isProvider()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return providerLookupPort.findProviderIdByUserId(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No provider profile"));
    }
}
