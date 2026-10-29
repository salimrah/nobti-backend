package ma.noba.payment.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.payment.domain.model.PaymentWebhookEvent;
import ma.noba.payment.domain.port.PaymentWebhookEventRepository;
import ma.noba.shared.application.dto.PagedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PaymentApplicationService payments;

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

    @Transactional
    public void markProcessed(UUID id) {
        webhooks.findById(id).ifPresent(event -> {
            event.markProcessed();
            webhooks.save(event);
        });
    }

    @Transactional
    public void markFailed(UUID id, String error) {
        webhooks.findById(id).ifPresent(event -> {
            event.markFailed(error);
            webhooks.save(event);
        });
    }

    @Transactional
    public PaymentWebhookEvent replay(UUID id) {
        PaymentWebhookEvent event = webhooks.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Webhook event not found"));
        if (event.getPaymentIntentId() == null || event.getEventType() == null) {
            throw new IllegalArgumentException("This event carries no payment to replay");
        }
        event.setReplayCount(event.getReplayCount() + 1);
        try {
            switch (event.getEventType()) {
                case "payment_intent.succeeded" ->
                        payments.handlePaymentSuccess(event.getPaymentIntentId(), event.getChargeId());
                case "payment_intent.payment_failed" ->
                        payments.handlePaymentFailure(event.getPaymentIntentId(), event.getFailureMessage());
                default -> throw new IllegalArgumentException(
                        "Event type " + event.getEventType() + " has no replay handler");
            }
            event.markProcessed();
            log.info("Replayed webhook {} ({})", event.getId(), event.getEventType());
        } catch (Exception e) {
            event.markFailed(e.getMessage());
            log.warn("Webhook replay failed for {}: {}", event.getId(), e.getMessage());
        }
        return webhooks.save(event);
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
