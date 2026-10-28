package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.WhatsAppGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Selected with {@code OTP_WHATSAPP_PROVIDER=none}; keeps the context bootable without WhatsApp. */
@Component
@ConditionalOnProperty(name = "otp.providers.whatsapp.active", havingValue = "none")
public class DisabledWhatsAppProvider implements WhatsAppGateway {

    private static final Logger log = LoggerFactory.getLogger(DisabledWhatsAppProvider.class);

    @Override
    public void sendMessage(String phoneNumber, String messageBody) {
        log.warn("WhatsApp provider is disabled (OTP_WHATSAPP_PROVIDER=none); refusing to send");
        throw new IllegalStateException("WhatsApp provider is disabled");
    }
}
