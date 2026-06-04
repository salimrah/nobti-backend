package ma.noba.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class TwilioHealthIndicator implements HealthIndicator {

    private final String accountSid;
    private final String authToken;

    public TwilioHealthIndicator(
            @org.springframework.beans.factory.annotation.Value("${twilio.accountSid:}") String accountSid,
            @org.springframework.beans.factory.annotation.Value("${twilio.authToken:}") String authToken) {
        this.accountSid = accountSid;
        this.authToken = authToken;
    }

    @Override
    public Health health() {
        if (accountSid == null || accountSid.isBlank()) {
            return Health.unknown()
                    .withDetail("twilio", "not configured")
                    .withDetail("hint", "Set TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN environment variables")
                    .build();
        }

        try {
            boolean valid = accountSid.startsWith("AC") && accountSid.length() == 34;
            if (valid) {
                return Health.up()
                        .withDetail("twilio", "configured")
                        .withDetail("accountSid", maskAccountSid(accountSid))
                        .build();
            } else {
                return Health.down()
                        .withDetail("twilio", "invalid configuration")
                        .withDetail("error", "Account SID format is invalid")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("twilio", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private String maskAccountSid(String sid) {
        return sid.substring(0, 6) + "..." + sid.substring(sid.length() - 4);
    }
}
