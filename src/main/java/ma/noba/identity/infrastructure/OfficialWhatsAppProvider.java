package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.WhatsAppGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "otp.providers.whatsapp.active", havingValue = "official-api")
public class OfficialWhatsAppProvider implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(OfficialWhatsAppProvider.class);

    @Override
    public void sendMessage(String phoneNumber, String messageBody) {
        log.warn("Official WhatsApp API provider not implemented — message not sent to {}", phoneNumber);
    }

}