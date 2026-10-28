package ma.noba.app.push;

import ma.noba.shared.domain.port.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dev / no-credentials adapter: logs the notification instead of calling a provider.
 * Selected by default ({@code app.notification.provider=log}).
 */
@Component
@ConditionalOnProperty(name = "app.notification.provider", havingValue = "log", matchIfMissing = true)
public class LogPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(LogPushGateway.class);

    @Override
    public void send(String token, String title, String body, Map<String, String> data) {
        log.info("[MOCK] Push to {} — {}: {} {}", mask(token), title, body, data);
    }

    private static String mask(String token) {
        return token == null || token.length() < 8 ? "***" : token.substring(0, 6) + "…";
    }
}
