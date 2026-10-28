package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies provider delivery receipts (FCM, SMS gateway, email provider) to a delivery. Receipts are
 * idempotent: re-delivering the same status never duplicates events or moves timestamps backwards.
 */
@Service
@RequiredArgsConstructor
public class NotificationReceiptService {

    private final NotificationDeliveryRepository deliveries;
    private final NotificationEventRepository events;

    public enum ReceiptStatus {
        SENT, DELIVERED, FAILED
    }

    public record Receipt(
            UUID deliveryId,
            String providerMessageId,
            ReceiptStatus status,
            Instant occurredAt,
            String errorCode,
            String errorMessage) {
    }

    public record Result(UUID deliveryId, DeliveryStatus status) {
    }

    @Transactional
    public Optional<Result> record(Receipt receipt) {
        Optional<NotificationDelivery> found = receipt.deliveryId() != null
                ? deliveries.findById(receipt.deliveryId())
                : deliveries.findByProviderMessageId(receipt.providerMessageId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        NotificationDelivery delivery = found.get();
        if (receipt.providerMessageId() != null && !receipt.providerMessageId().isBlank()) {
            delivery.setProviderMessageId(receipt.providerMessageId());
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("channel", delivery.getChannel().name());
        metadata.put("status", receipt.status().name());
        if (receipt.occurredAt() != null) {
            metadata.put("occurredAt", receipt.occurredAt().toString());
        }

        switch (receipt.status()) {
            case DELIVERED -> {
                if (delivery.getStatus() != DeliveryStatus.DELIVERED) {
                    delivery.markDelivered();
                    if (receipt.occurredAt() != null) {
                        delivery.setDeliveredAt(receipt.occurredAt());
                    }
                    events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                            NotificationEventType.DELIVERED, metadata));
                }
            }
            case SENT -> {
                if (delivery.getStatus() != DeliveryStatus.SENT && delivery.getStatus() != DeliveryStatus.DELIVERED) {
                    delivery.markSent(delivery.getProviderMessageId(), delivery.getLatencyMs());
                    if (receipt.occurredAt() != null) {
                        delivery.setSentAt(receipt.occurredAt());
                    }
                    events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                            NotificationEventType.SENT, metadata));
                }
            }
            case FAILED -> {
                String code = receipt.errorCode() == null ? "PROVIDER_FAILURE" : receipt.errorCode();
                delivery.markFailed(code, receipt.errorMessage(), null);
                metadata.put("errorCode", code);
                events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                        NotificationEventType.FAILED, metadata));
            }
        }

        deliveries.saveDelivery(delivery);
        return Optional.of(new Result(delivery.getId(), delivery.getStatus()));
    }
}
