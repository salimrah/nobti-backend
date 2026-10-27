package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "otp.providers.sms.active", havingValue = "aws-sns")
public class AwsSnsSmsProvider implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(AwsSnsSmsProvider.class);

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        log.warn("AWS SNS SMS provider not implemented — SMS not sent to {}", toPhoneNumber);
    }
}
