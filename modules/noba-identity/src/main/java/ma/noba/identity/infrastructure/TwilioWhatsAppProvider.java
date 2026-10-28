package ma.noba.identity.infrastructure;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import ma.noba.config.ResilienceConfig;
import ma.noba.identity.domain.service.WhatsAppGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "otp.providers.whatsapp.active", havingValue = "twilio")
public class TwilioWhatsAppProvider implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppProvider.class);

    @Value("${otp.providers.whatsapp.from:}") String whatsappFrom;

    @Override
    @CircuitBreaker(name = ResilienceConfig.EXTERNAL_SERVICE, fallbackMethod = "sendFallback")
    @Retry(name = ResilienceConfig.EXTERNAL_SERVICE)
    public void sendMessage(String toPhoneNumber, String messageBody) {
        Message.creator(
                new PhoneNumber("whatsapp:" + toPhoneNumber.replace("whatsapp:", "")),
                new PhoneNumber(whatsappFrom),
                messageBody
        ).create();
    }

    public void sendFallback(String toPhoneNumber, String messageBody, Throwable t) {
        log.error("Twilio WhatsApp sending failed for {} — circuit breaker fallback: {}", toPhoneNumber, t.getMessage());
    }
}