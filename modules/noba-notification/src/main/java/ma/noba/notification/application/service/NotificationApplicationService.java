package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.NotificationResponse;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.notification.domain.port.NotificationPreferencePort;
import ma.noba.notification.domain.port.NotificationRepository;
import ma.noba.shared.application.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the notification intent and its channel fan-out. Feature modules call the
 * {@link NotificationPort} contract; the inbox API reads from here.
 */
@Service
@RequiredArgsConstructor
public class NotificationApplicationService implements NotificationPort {

    private final NotificationRepository notifications;
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEventRepository events;
    private final NotificationPreferencePort preferences;

    @Override
    @Transactional
    public UUID send(Command command) {
        if (command.dedupeKey() != null && !command.dedupeKey().isBlank()) {
            var existing = notifications.findByDedupeKey(command.dedupeKey());
            if (existing.isPresent()) {
                return existing.get().getId();
            }
        }

        NotificationCategory category = command.category() == null
                ? NotificationCategory.TRANSACTIONAL
                : NotificationCategory.valueOf(command.category().name());
        Notification notification = Notification.create(
                command.userId(),
                command.type(),
                category,
                command.priority(),
                command.title(),
                command.body(),
                command.data(),
                command.dedupeKey(),
                command.locale());
        notification = notifications.saveNotification(notification);
        UUID notificationId = notification.getId();

        // The inbox item is delivered immediately; external channels are queued below.
        deliveries.saveDelivery(NotificationDelivery.inApp(notificationId));
        events.record(NotificationEvent.of(notificationId, null,
                NotificationEventType.CREATED, Map.of("type", notification.getType())));

        boolean pending = false;
        boolean suppressed = false;
        for (NotificationPort.Channel requested : command.channels()) {
            if (requested == NotificationPort.Channel.IN_APP) {
                continue;
            }
            NotificationChannel channel = NotificationChannel.valueOf(requested.name());
            String recipient = recipientFor(channel, command);
            NotificationDelivery delivery = NotificationDelivery.queued(
                    notificationId, channel, channel.name().toLowerCase(), recipient);

            if (!preferences.isEnabled(command.userId(), category, channel)) {
                delivery.markSuppressed("PREFERENCE_DISABLED", "channel disabled by the user");
                deliveries.saveDelivery(delivery);
                events.record(NotificationEvent.of(notificationId, delivery.getId(),
                        NotificationEventType.SUPPRESSED,
                        Map.of("channel", channel.name(), "reason", "PREFERENCE_DISABLED")));
                suppressed = true;
                continue;
            }

            Instant deferredUntil = category == NotificationCategory.MARKETING
                    ? deferredUntil(preferences.quietHours(command.userId())).orElse(null)
                    : null;
            if (deferredUntil != null) {
                delivery.setNextAttemptAt(deferredUntil);
            }
            deliveries.saveDelivery(delivery);
            Map<String, Object> queueMetadata = new HashMap<>();
            queueMetadata.put("channel", channel.name());
            if (deferredUntil != null) {
                queueMetadata.put("deferredUntil", deferredUntil.toString());
            }
            events.record(NotificationEvent.of(notificationId, delivery.getId(),
                    NotificationEventType.QUEUED, queueMetadata));
            pending = true;
        }

        notification.setStatus(pending ? NotificationStatus.QUEUED
                : suppressed ? NotificationStatus.PARTIAL : NotificationStatus.SENT);
        notifications.saveNotification(notification);
        return notificationId;
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> list(UUID userId, Pageable pageable) {
        Page<Notification> page = notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.of(
                page.map(NotificationResponse::from).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public boolean markRead(UUID notificationId, UUID userId) {
        int updated = notifications.markRead(notificationId, userId, Instant.now());
        if (updated > 0) {
            events.record(NotificationEvent.of(notificationId, null,
                    NotificationEventType.READ, Map.of("userId", userId.toString())));
        }
        return updated > 0;
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notifications.markAllRead(userId, Instant.now());
    }

    /** Returns the end of quiet hours when the user is currently inside them. */
    private static Optional<Instant> deferredUntil(Optional<NotificationPreferencePort.QuietHours> quietHours) {
        if (quietHours.isEmpty()) {
            return Optional.empty();
        }
        NotificationPreferencePort.QuietHours window = quietHours.get();
        ZoneId zone;
        try {
            zone = ZoneId.of(window.timezone());
        } catch (Exception e) {
            zone = ZoneId.systemDefault();
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalTime time = now.toLocalTime();
        boolean quiet = window.start().isBefore(window.end())
                ? !time.isBefore(window.start()) && time.isBefore(window.end())
                : !time.isBefore(window.start()) || time.isBefore(window.end());
        if (!quiet) {
            return Optional.empty();
        }
        ZonedDateTime end = now.with(window.end());
        if (!end.isAfter(now)) {
            end = end.plusDays(1);
        }
        return Optional.of(end.toInstant());
    }

    private static String recipientFor(NotificationChannel channel, Command command) {
        return switch (channel) {
            case EMAIL -> maskEmail(command.email());
            case SMS, WHATSAPP -> maskPhone(command.phone());
            default -> null;
        };
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) return null;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "***" + phone.substring(phone.length() - 3);
    }
}
