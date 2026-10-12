package ma.noba.queue.interfaces.rest;

import ma.noba.queue.application.dto.JoinQueueResponse;
import ma.noba.queue.application.dto.QueueMemberResponse;
import ma.noba.queue.application.dto.QueueStatusResponse;
import ma.noba.queue.application.service.QueueApplicationService;
import jakarta.validation.constraints.Positive;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.queue.domain.model.QueuePriority;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.user.UserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/queues")
@Validated
public class QueueController {

    private final QueueApplicationService queueService;
    private final ProviderLookupPort providerLookupPort;

    public QueueController(QueueApplicationService queueService, ProviderLookupPort providerLookupPort) {
        this.queueService = queueService;
        this.providerLookupPort = providerLookupPort;
    }

    @PostMapping("/providers/{providerId}/join")
    public ResponseEntity<JoinQueueResponse> joinQueue(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false, defaultValue = "1") @Positive Integer partySize,
            @RequestParam(required = false) QueuePriority priority) {
        JoinQueueResponse response = queueService.joinQueue(providerId, user.id(), serviceId, employeeId, groupId, partySize, priority);
        return ResponseEntity.created(URI.create("/api/v1/queues/" + response.queueId()))
                .body(response);
    }

    @DeleteMapping("/providers/{providerId}/leave")
    public ResponseEntity<Void> leaveQueue(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        queueService.leaveQueue(providerId, user.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/providers/{providerId}/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@PathVariable UUID providerId) {
        QueueStatusResponse response = queueService.getQueueStatus(providerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/{providerId}/members")
    public ResponseEntity<PagedResponse<QueueMemberResponse>> getQueueMembers(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId,
            Pageable pageable) {
        // The queue is keyed by the provider entity id, which is distinct from the owner's user id.
        if (!providerId.equals(currentProviderIdOrNull(user))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        PagedResponse<QueueMemberResponse> responses = queueService.getQueueMembers(providerId, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/providers/{providerId}/my-position")
    public ResponseEntity<QueueMemberResponse> getMyPosition(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID providerId) {
        QueueMemberResponse response = queueService.getMyQueuePosition(providerId, user.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/today")
    public ResponseEntity<List<QueueMemberResponse>> getMyQueuesToday(@AuthenticationPrincipal UserPrincipal user) {
        List<QueueMemberResponse> responses = queueService.getMyQueuesToday(user.id());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/providers/self/pause")
    public ResponseEntity<Void> pauseQueue(@AuthenticationPrincipal UserPrincipal user) {
        queueService.pauseQueue(requireProviderId(user));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/providers/self/resume")
    public ResponseEntity<Void> resumeQueue(@AuthenticationPrincipal UserPrincipal user) {
        queueService.resumeQueue(requireProviderId(user));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/providers/self/close")
    public ResponseEntity<Void> closeQueue(@AuthenticationPrincipal UserPrincipal user) {
        queueService.closeQueue(requireProviderId(user));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/providers/self/next")
    public ResponseEntity<Void> advanceQueue(@AuthenticationPrincipal UserPrincipal user) {
        queueService.advanceQueue(requireProviderId(user));
        return ResponseEntity.ok().build();
    }

    /** Mark a waiting client as a no-show; the next waiting client is auto-promoted. */
    @PostMapping("/providers/self/members/{memberId}/no-show")
    public ResponseEntity<Void> markNoShow(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID memberId) {
        queueService.markNoShow(requireProviderId(user), memberId);
        return ResponseEntity.ok().build();
    }

    /** Resolves the provider entity id for the authenticated provider, or 403 if not a provider. */
    private UUID requireProviderId(UserPrincipal user) {
        if (user == null || !user.isProvider()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return providerLookupPort.findProviderIdByUserId(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No provider profile"));
    }

    private UUID currentProviderIdOrNull(UserPrincipal user) {
        if (user == null || !user.isProvider()) {
            return null;
        }
        return providerLookupPort.findProviderIdByUserId(user.id()).orElse(null);
    }
}
