package ma.noba.shared.domain.port;

import java.util.Map;

/**
 * Provider-neutral push notification contract. Implemented by FCM today; OneSignal, Pusher Beams
 * or any other provider can be dropped in without touching the reminder/queue use cases.
 */
public interface PushGateway {

    /**
     * Sends a notification to a device token.
     *
     * @param data additional key/value payload (for example {@code type}, {@code reminderId})
     * @throws ma.noba.shared.domain.exception.PushDeliveryException when delivery fails
     */
    void send(String token, String title, String body, Map<String, String> data);
}
