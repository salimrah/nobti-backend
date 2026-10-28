package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.AdminDeliveryView;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.shared.application.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Admin view of delivery attempts plus a safe retry for terminal failures. */
@Service
@RequiredArgsConstructor
public class NotificationDeliveryAdminService {

    private final NotificationDeliveryRepository deliveries;
    private final NotificationEventRepository events;

    @Transactional(readOnly = true)
    public PagedResponse<AdminDeliveryView> search(String status, String channel, int page, int size) {
        Page<NotificationDeliveryRepository.DeliveryWithType> result = deliveries.searchWithType(
                parse(status, DeliveryStatus.class),
                parse(channel, NotificationChannel.class),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PagedResponse.of(
                result.getContent().stream()
                        .map(row -> toView(row.delivery(), row.notificationType()))
                        .toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public AdminDeliveryView retry(UUID deliveryId) {
        NotificationDelivery delivery = deliveries.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new IllegalArgumentException("Only failed deliveries can be retried");
        }
        delivery.setStatus(DeliveryStatus.QUEUED);
        delivery.setNextAttemptAt(Instant.now());
        delivery.setErrorCode(null);
        delivery.setErrorMessage(null);
        deliveries.saveDelivery(delivery);
        events.record(NotificationEvent.of(delivery.getNotificationId(), delivery.getId(),
                NotificationEventType.QUEUED, Map.of("retry", true, "channel", delivery.getChannel().name())));
        return toView(delivery, null);
    }

    private static AdminDeliveryView toView(NotificationDelivery delivery, String notificationType) {
        return new AdminDeliveryView(
                delivery.getId(),
                delivery.getNotificationId(),
                notificationType,
                delivery.getChannel() != null ? delivery.getChannel().name() : null,
                delivery.getProvider(),
                delivery.getStatus() != null ? delivery.getStatus().name() : null,
                delivery.getAttempt(),
                delivery.getProviderMessageId(),
                delivery.getErrorCode(),
                delivery.getErrorMessage(),
                delivery.getQueuedAt(),
                delivery.getSentAt(),
                delivery.getDeliveredAt(),
                delivery.getNextAttemptAt(),
                delivery.getCreatedAt());
    }

    private static <E extends Enum<E>> E parse(String value, Class<E> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
