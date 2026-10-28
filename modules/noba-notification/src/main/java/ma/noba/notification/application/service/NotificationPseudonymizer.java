package ma.noba.notification.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Turns a user id into a stable pseudonym for the warehouse export. HMAC-SHA256 keyed with the
 * service secret: the same user always maps to the same value (so the warehouse can count unique
 * users) but the original id cannot be recovered without the key.
 */
@Component
public class NotificationPseudonymizer {

    private static final int HEX_LENGTH = 32;

    private final byte[] key;

    public NotificationPseudonymizer(
            @Value("${notification.export.pseudonym-secret:}") String exportSecret,
            @Value("${security.pii.secret-b64:}") String piiSecret) {
        String secret = exportSecret != null && !exportSecret.isBlank() ? exportSecret : piiSecret;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "notification.export.pseudonym-secret (or security.pii.secret-b64) must be configured");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String apply(UUID userId) {
        if (userId == null) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(userId.toString().getBytes(StandardCharsets.UTF_8));
            return "u_" + HexFormat.of().formatHex(digest).substring(0, HEX_LENGTH);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to pseudonymize user id", e);
        }
    }
}
