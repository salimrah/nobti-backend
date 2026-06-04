package ma.noba.identity.infrastructure.email;

import ma.noba.identity.domain.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "sendgrid")
public class SendGridEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailSender.class);
    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    private final String from;
    private final String apiKey;

    public SendGridEmailSender(@Value("${email.from:noreply@noba.ma}") String from,
                                @Value("${sendgrid.api-key:}") String apiKey) {
        this.from = from;
        this.apiKey = apiKey;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (apiKey.isBlank()) {
            log.warn("SendGrid API key not configured. Email to {} is simulated.", to);
            return;
        }
        String contentType = body.trim().startsWith("<") ? "text/html" : "text/plain";
        String payload = """
            {
                "personalizations": [{"to": [{"email": "%s"}]}],
                "from": {"email": "%s"},
                "subject": "%s",
                "content": [{"type": "%s", "value": "%s"}]
            }
            """ .formatted(
                escapeJson(to), escapeJson(from),
                escapeJson(subject), contentType, escapeJson(body));

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(SENDGRID_API_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            if (status >= 400) {
                log.warn("SendGrid returned {} for email to {}", status, to);
            } else {
                log.debug("Email sent to {} via SendGrid, status={}", to, status);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via SendGrid to " + to, e);
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
