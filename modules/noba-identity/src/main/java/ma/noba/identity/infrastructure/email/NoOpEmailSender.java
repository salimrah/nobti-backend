package ma.noba.identity.infrastructure.email;

import ma.noba.identity.domain.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "log")
public class NoOpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("[NoOpEmailSender] Would send email to: {}, subject: {}, body: {}", to, subject, body);
    }
}
