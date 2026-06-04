package ma.noba.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FirebaseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            FirebaseApp.getInstance();
            FirebaseMessaging.getInstance();
            return Health.up()
                    .withDetail("firebase", "connected")
                    .withDetail("messaging", "available")
                    .build();
        } catch (IllegalStateException e) {
            return Health.down()
                    .withDetail("firebase", "not initialized")
                    .withDetail("error", e.getMessage())
                    .withDetail("hint", "Add serviceAccountKey.json to src/main/resources")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("firebase", "error")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
