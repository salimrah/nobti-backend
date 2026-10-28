package ma.noba.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.noba.identity.domain.service.SmsGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;

/**
 * Generic HTTP SMS gateway: lets any local Moroccan gateway (or a SaaS with a REST API) be used
 * by configuration, without a new adapter. Selected with {@code OTP_SMS_PROVIDER=http}.
 *
 * <p>The body template supports {@code {to}}, {@code {message}} and {@code {from}} placeholders.
 * Values are JSON-escaped, so the default JSON template is safe for arbitrary OTP text.
 */
@Component
@ConditionalOnProperty(name = "otp.providers.sms.active", havingValue = "http")
public class HttpSmsProvider implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpSmsProvider.class);
    private static final String DEFAULT_BODY_TEMPLATE =
            "{\"to\":\"{to}\",\"message\":\"{message}\",\"from\":\"{from}\"}";

    private final RestClient client;
    private final String url;
    private final String method;
    private final MediaType contentType;
    private final String authHeader;
    private final String authToken;
    private final String bodyTemplate;
    private final String from;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpSmsProvider(
            @Value("${otp.providers.sms.http.url:}") String url,
            @Value("${otp.providers.sms.http.method:POST}") String method,
            @Value("${otp.providers.sms.http.content-type:application/json}") String contentType,
            @Value("${otp.providers.sms.http.auth-header:Authorization}") String authHeader,
            @Value("${otp.providers.sms.http.auth-token:}") String authToken,
            @Value("${otp.providers.sms.http.body-template:}") String bodyTemplate,
            @Value("${otp.providers.sms.from:}") String from,
            @Value("${otp.providers.sms.http.timeout-ms:5000}") int timeoutMs) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "otp.providers.sms.http.url is required when OTP_SMS_PROVIDER=http");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.client = RestClient.builder().requestFactory(factory).build();
        this.url = url;
        this.method = method;
        this.contentType = MediaType.parseMediaType(contentType);
        this.authHeader = authHeader;
        this.authToken = authToken;
        this.bodyTemplate = bodyTemplate == null || bodyTemplate.isBlank()
                ? DEFAULT_BODY_TEMPLATE : bodyTemplate;
        this.from = from;
    }

    @Override
    public void sendSms(String toPhoneNumber, String messageBody) {
        String body = bodyTemplate
                .replace("{to}", jsonEscape(toPhoneNumber))
                .replace("{message}", jsonEscape(messageBody))
                .replace("{from}", jsonEscape(from));

        var request = client.method(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)))
                .uri(url)
                .contentType(contentType);
        if (authToken != null && !authToken.isBlank()) {
            request = request.header(authHeader, authToken);
        }

        try {
            request.body(body).retrieve().toBodilessEntity();
            log.info("OTP SMS sent via HTTP gateway to {}", mask(toPhoneNumber));
        } catch (RestClientResponseException e) {
            log.error("HTTP SMS gateway returned {} for {}", e.getStatusCode(), mask(toPhoneNumber));
            throw new IllegalStateException("SMS gateway error: " + e.getStatusCode(), e);
        }
    }

    private String jsonEscape(String value) {
        try {
            String quoted = objectMapper.writeValueAsString(value == null ? "" : value);
            return quoted.substring(1, quoted.length() - 1);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode SMS value", e);
        }
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 3);
    }
}
