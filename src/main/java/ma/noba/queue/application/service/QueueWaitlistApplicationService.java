package ma.noba.queue.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.port.ProviderLookupPort;
import ma.noba.queue.application.dto.WaitlistEntryResponse;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueWaitlistEntry;
import ma.noba.queue.domain.model.QueueWaitlistStatus;
import ma.noba.queue.domain.port.service.ClientDisplayNamePort;
import ma.noba.queue.domain.port.service.QueueNotificationPort;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.queue.domain.port.service.QueueWaitlistRepository;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.NotFoundException;
import ma.noba.shared.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages the persistent waitlist: clients register interest when they cannot join a queue
 * (closed/paused) and are notified when the provider opens it.
 */
@Service
@RequiredArgsConstructor
public class QueueWaitlistApplicationService {

    private static final Logger log = LoggerFactory.getLogger(QueueWaitlistApplicationService.class);

    private final QueueWaitlistRepository waitlistRepository;
    private final QueueRepository queueRepository;
    private final QueueNotificationPort notificationPort;
    private final ProviderLookupPort providerLookupPort;
    private final ClientDisplayNamePort clientDisplayNamePort;

    @Transactional
    public WaitlistEntryResponse joinWaitlist(UUID providerId, UUID clientId) {
        Queue queue = queueRepository.findByProviderId(providerId).orElse(null);
        if (queue != null && queue.getActiveMembers().stream().anyMatch(m -> m.getClientId().equals(clientId))) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "You are already in the queue");
        }
        if (waitlistRepository.findActiveByProviderAndClient(providerId, clientId).isPresent()) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, "You are already on the waitlist");
        }
        QueueWaitlistEntry entry = waitlistRepository.save(new QueueWaitlistEntry(providerId, clientId));
        return toResponse(entry, providerName(providerId), null, 0);
    }

    @Transactional
    public void leaveWaitlist(UUID providerId, UUID clientId) {
        QueueWaitlistEntry entry = waitlistRepository.findActiveByProviderAndClient(providerId, clientId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.QUEUE_NOT_FOUND, "No active waitlist entry"));
        entry.cancel();
        waitlistRepository.save(entry);
    }

    /** Marks any active waitlist entry as converted (called when the client joins the queue). */
    @Transactional
    public void markConverted(UUID providerId, UUID clientId) {
        waitlistRepository.findActiveByProviderAndClient(providerId, clientId).ifPresent(entry -> {
            entry.convert();
            waitlistRepository.save(entry);
        });
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getMyWaitlist(UUID clientId) {
        return waitlistRepository.findActiveByClient(clientId).stream()
                .map(e -> toResponse(e, providerName(e.getProviderId()), null, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getProviderWaitlist(UUID providerId) {
        List<QueueWaitlistEntry> entries = waitlistRepository.findActiveByProvider(providerId);
        Set<UUID> ids = entries.stream().map(QueueWaitlistEntry::getClientId).collect(Collectors.toSet());
        Map<UUID, String> names = ids.isEmpty() ? Map.of() : clientDisplayNamePort.findDisplayNames(ids);
        String providerName = providerName(providerId);
        return IntStream.range(0, entries.size())
                .mapToObj(i -> toResponse(entries.get(i), providerName, names.get(entries.get(i).getClientId()), i + 1))
                .toList();
    }

    /**
     * Notifies up to {@code limit} oldest waiting clients that the queue is open / has room.
     * A failed push never aborts the others.
     */
    @Transactional
    public int notifyWaitlist(UUID providerId, int limit) {
        if (limit <= 0) {
            return 0;
        }
        List<QueueWaitlistEntry> waiting = waitlistRepository.findActiveByProvider(providerId).stream()
                .filter(e -> e.getStatus() == QueueWaitlistStatus.WAITING)
                .limit(limit)
                .toList();
        for (QueueWaitlistEntry entry : waiting) {
            entry.markNotified();
            waitlistRepository.save(entry);
            try {
                notificationPort.notifyWaitlistOpen(entry.getClientId(), providerId);
            } catch (Exception e) {
                log.warn("Waitlist notification failed for client {} at provider {}: {}",
                        entry.getClientId(), providerId, e.getMessage());
            }
        }
        return waiting.size();
    }

    private String providerName(UUID providerId) {
        return providerLookupPort.findProviderName(providerId).orElse(null);
    }

    private WaitlistEntryResponse toResponse(QueueWaitlistEntry entry, String providerName, String clientName, int position) {
        return new WaitlistEntryResponse(
                entry.getId().toString(),
                entry.getProviderId().toString(),
                providerName,
                entry.getClientId().toString(),
                clientName,
                entry.getStatus().name(),
                position,
                entry.getCreatedAt(),
                entry.getNotifiedAt());
    }
}
