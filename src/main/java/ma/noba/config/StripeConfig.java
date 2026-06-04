package ma.noba.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
@ConditionalOnExpression("'${app.stripe.secret-key:}' != ''")
public class StripeConfig {

    private final String secretKey;
    private final String webhookSecret;
    private final String publishableKey;

    public StripeConfig(
        @Value("${app.stripe.secret-key:}") String secretKey,
        @Value("${app.stripe.webhook-secret:}") String webhookSecret,
        @Value("${app.stripe.publishable-key:}") String publishableKey
    ) {
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.publishableKey = publishableKey;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialized with publishable key: {}...", publishableKey.substring(0, Math.min(8, publishableKey.length())));
    }
}
