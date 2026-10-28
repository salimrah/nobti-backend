package ma.noba.app.notification;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationChannelPort;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.shared.domain.exception.PushDeliveryException;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import ma.noba.shared.domain.port.PushGateway;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Push channel adapter: resolves the user's token and sends through the provider-neutral
 * {@link PushGateway} (FCM today, another provider later).
 */
@Component
@RequiredArgsConstructor
public class PushNotificationChannel implements NotificationChannelPort {

    private final PushGateway pushGateway;
    private final FcmTokenQueryPort tokens;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(Notification notification, NotificationDelivery delivery) {
        String token = tokens.findFcmTokenByUserId(notification.getUserId()).orElse(null);
        if (token == null) {
            throw new NotificationDeliveryException(true, false, "No push token for user", null);
        }
        try {
            pushGateway.send(token, notification.getTitle(), notification.getBody(),
                    asStringMap(notification.getData()));
        } catch (PushDeliveryException e) {
            if (e.isInvalidToken()) {
                tokens.invalidateToken(token);
                throw new NotificationDeliveryException(true, false, e.getMessage(), e);
            }
            throw new NotificationDeliveryException(false, true, e.getMessage(), e);
        }
    }

    private static Map<String, String> asStringMap(Map<String, Object> data) {
        Map<String, String> out = new HashMap<>();
        if (data != null) {
            data.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, String.valueOf(value));
                }
            });
        }
        return out;
    }
}
