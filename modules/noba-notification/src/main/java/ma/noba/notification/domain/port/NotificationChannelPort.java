package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;

/**
 * Implemented by the composition root for each external channel (push, email, SMS).
 * IN_APP is handled by the notification service itself.
 */
public interface NotificationChannelPort {

    NotificationChannel channel();

    void send(Notification notification, NotificationDelivery delivery) throws NotificationDeliveryException;
}
