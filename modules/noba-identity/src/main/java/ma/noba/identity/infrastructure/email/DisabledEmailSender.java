package ma.noba.identity.infrastructure.email;

import ma.noba.identity.domain.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Selected with {@code EMAIL_PROVIDER=none} (staging default). Keeps the application context
 * bootable without an email provider while failing sends explicitly instead of silently dropping.
 */
@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "none")
public class DisabledEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(DisabledEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.warn("Email provider is disabled (EMAIL_PROVIDER=none); refusing to send '{}'", subject);
        throw new IllegalStateException("Email provider is disabled");
    }
}
