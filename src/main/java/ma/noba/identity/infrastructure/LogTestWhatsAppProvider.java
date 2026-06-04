package ma.noba.identity.infrastructure;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.service.WhatsAppGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "otp.providers.whatsapp.active", havingValue = "log")
public class LogTestWhatsAppProvider implements WhatsAppGateway {

    private static final Log log = LogFactory.getLog(LogTestWhatsAppProvider.class);

    @Override
    public void sendMessage(String phoneNumber, String messageBody) {
        log.info(messageBody);
    }
}
