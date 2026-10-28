package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.service.EmailSender;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationChannelPort;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.notification.domain.port.NotificationRecipientPort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannelPort {

    private final EmailSender emailSender;
    private final NotificationRecipientPort recipients;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {
        String to = recipients.email(notification.getUserId())
                .orElseThrow(() -> new NotificationDeliveryException(true, false, "No email for user", null));
        try {
            emailSender.send(to, notification.getTitle(), notification.getBody());
        } catch (Exception e) {
            throw new NotificationDeliveryException(false, true, e.getMessage(), e);
        }
    }
}
