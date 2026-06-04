package ma.noba.identity.infrastructure;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import ma.noba.config.ResilienceConfig;
import ma.noba.identity.domain.service.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "otp.providers.sms.active", havingValue = "twilio")
@RequiredArgsConstructor
public class TwilioSmsProvider implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

    @Value("${providers.twilio.accountSid}") String accountSid;
    @Value("${providers.twilio.authToken}") String authToken;
    @Value("${providers.twilio.smsFrom}") String smsFrom;

    @PostConstruct
    void init(){ Twilio.init(accountSid, authToken); }

    @Override
    @CircuitBreaker(name = ResilienceConfig.EXTERNAL_SERVICE, fallbackMethod = "sendFallback")
    @Retry(name = ResilienceConfig.EXTERNAL_SERVICE)
    public void sendSms(String toPhoneNumber, String messageBody) {
        Message.creator(
                new com.twilio.type.PhoneNumber(toPhoneNumber),
                new com.twilio.type.PhoneNumber(smsFrom),
                        messageBody)
        .create();
    }

    public void sendFallback(String toPhoneNumber, String messageBody, Throwable t) {
        log.error("Twilio SMS sending failed for {} — circuit breaker fallback: {}", toPhoneNumber, t.getMessage());
    }
}
