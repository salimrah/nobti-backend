package ma.noba.app.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import ma.noba.shared.domain.exception.PushDeliveryException;
import ma.noba.shared.domain.port.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FCM adapter. Selected with {@code app.notification.provider=fcm}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.provider", havingValue = "fcm")
public class FcmPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);

    private final FirebaseMessaging messaging;

    public FcmPushGateway(ObjectProvider<FirebaseMessaging> messaging) {
        this.messaging = messaging.getIfAvailable();
    }

    @Override
    public void send(String token, String title, String body, Map<String, String> data) {
        if (messaging == null) {
            throw new PushDeliveryException(false, "Firebase is not configured", null);
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null) {
                data.forEach(builder::putData);
            }
            messaging.send(builder.build());
        } catch (FirebaseMessagingException e) {
            String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
            boolean invalidToken = "UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code);
            log.error("FCM send failed (code={}) for token {}: {}", code, mask(token), e.getMessage());
            throw new PushDeliveryException(invalidToken, "Push delivery failed: " + e.getMessage(), e);
        }
    }

    private static String mask(String token) {
        return token == null || token.length() < 8 ? "***" : token.substring(0, 6) + "…";
    }
}
