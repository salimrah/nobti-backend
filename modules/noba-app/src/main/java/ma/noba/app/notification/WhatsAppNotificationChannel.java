package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.service.WhatsAppGateway;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationChannelPort;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.notification.domain.port.NotificationRecipientPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WhatsAppNotificationChannel implements NotificationChannelPort {

    private final WhatsAppGateway whatsAppGateway;
    private final NotificationRecipientPort recipients;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {
        String to = recipients.phone(notification.getUserId())
                .orElseThrow(() -> new NotificationDeliveryException(true, false, "No phone for user", null));
        try {
            whatsAppGateway.sendMessage(to, notification.getTitle() + ": " + notification.getBody());
        } catch (Exception e) {
            throw new NotificationDeliveryException(false, true, e.getMessage(), e);
        }
    }
}
