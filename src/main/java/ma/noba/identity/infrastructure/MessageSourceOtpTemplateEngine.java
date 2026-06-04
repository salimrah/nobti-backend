package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.service.OtpTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "otp.template.provider", havingValue = "messagesource", matchIfMissing = true)
public class MessageSourceOtpTemplateEngine implements OtpTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(MessageSourceOtpTemplateEngine.class);
    private static final String FALLBACK_EN = "Your Nob\u00e1 verification code: {code}";

    private final MessageSourceAccessor messages;

    public MessageSourceOtpTemplateEngine(MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    @Override
    public String render(String templateName, Map<String, Object> variables, Locale locale) {
        String key = "otp.template." + templateName;
        String template;
        try {
            template = messages.getMessage(key, locale);
        } catch (Exception e) {
            log.warn("No message found for key '{}' in locale '{}', using fallback", key, locale);
            template = FALLBACK_EN;
        }
        return apply(template, variables);
    }

    static String apply(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) return template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return template;
    }
}
