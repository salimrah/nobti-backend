package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Selected with {@code OTP_SMS_PROVIDER=none}; keeps the context bootable without an SMS provider. */
@Component
@ConditionalOnProperty(name = "otp.providers.sms.active", havingValue = "none")
public class DisabledSmsProvider implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(DisabledSmsProvider.class);

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        log.warn("SMS provider is disabled (OTP_SMS_PROVIDER=none); refusing to send");
        throw new IllegalStateException("SMS provider is disabled");
    }
}
