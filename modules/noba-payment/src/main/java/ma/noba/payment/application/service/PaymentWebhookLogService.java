package ma.noba.payment.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.model.PaymentWebhookEvent;
import ma.noba.payment.domain.port.PaymentWebhookEventRepository;
import ma.noba.payment.domain.port.PaymentWebhookReplayPort;
import ma.noba.shared.application.dto.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores every payment webhook so operations can inspect and replay failures. Replays reuse the
 * data captured at receipt time (already signature-verified) and go through the same idempotent
 * payment handlers.
 */
@Service
@RequiredArgsConstructor
public class PaymentWebhookLogService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookLogService.class);

    private final PaymentWebhookEventRepository webhooks;
    private final ObjectProvider<PaymentWebhookReplayPort> replayer;

    @Transactional
    public PaymentWebhookEvent record(String provider, String eventId, String eventType,
                                      String paymentIntentId, String chargeId, String failureMessage,
                                      String payload) {
        if (eventId != null) {
            Optional<PaymentWebhookEvent> existing = webhooks.findByProviderAndEventId(provider, eventId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider);
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setPaymentIntentId(paymentIntentId);
        event.setChargeId(chargeId);
        event.setFailureMessage(failureMessage);
        event.setPayload(payload);
        event.setStatus("RECEIVED");
        return webhooks.save(event);
    }

    @Transactional
    public PaymentWebhookEvent recordRejected(String provider, String eventId, String eventType,
                                              String payload, String error) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider);
        event.setEventId(eventId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus("REJECTED");
        event.setErrorMessage(error == null ? null : error.substring(0, Math.min(500, error.length())));
        event.setProcessedAt(java.time.Instant.now());
        return webhooks.save(event);
    }

    /**
     * Applies a stored event through the provider handler. Used both for live deliveries and for
     * manual replays; the outcome is recorded on the event row.
     */
    @Transactional
    public PaymentWebhookEvent process(UUID id) {
        PaymentWebhookEvent event = webhooks.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Webhook event not found"));
        PaymentWebhookReplayPort handler = replayer.getIfAvailable();
        if (handler == null) {
            event.markFailed("No webhook handler is configured");
            return webhooks.save(event);
        }
        try {
            handler.replay(event);
            event.markProcessed();
            log.info("Processed webhook {} ({})", event.getId(), event.getEventType());
        } catch (Exception e) {
            event.markFailed(e.getMessage());
            log.warn("Webhook {} failed: {}", event.getId(), e.getMessage());
        }
        return webhooks.save(event);
    }

    @Transactional
    public PaymentWebhookEvent replay(UUID id) {
        PaymentWebhookEvent event = webhooks.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Webhook event not found"));
        if (event.getEventType() == null || event.getPayload() == null) {
            throw new IllegalArgumentException("This event carries no replayable payload");
        }
        PaymentWebhookReplayPort handler = replayer.getIfAvailable();
        if (handler == null) {
            throw new IllegalStateException("No webhook replay handler is configured");
        }
        event.setReplayCount(event.getReplayCount() + 1);
        webhooks.save(event);
        return process(event.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<PaymentWebhookEvent> search(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<PaymentWebhookEvent> result = status == null || status.isBlank()
                ? webhooks.findAllByOrderByReceivedAtDesc(pageable)
                : webhooks.findByStatusOrderByReceivedAtDesc(
                        status.trim().toUpperCase(Locale.ROOT), pageable);
        return PagedResponse.of(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<PaymentWebhookEvent> find(UUID id) {
        return webhooks.findById(id);
    }
}
