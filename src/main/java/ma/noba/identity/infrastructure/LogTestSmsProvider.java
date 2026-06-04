package ma.noba.identity.infrastructure;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.service.SmsGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "otp.providers.sms.active", havingValue = "log")
public class LogTestSmsProvider implements SmsGateway {

    private static final Log log = LogFactory.getLog(LogTestSmsProvider.class);

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        log.info(messageBody);
    }
}
