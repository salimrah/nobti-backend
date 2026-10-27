package ma.noba.identity.domain.service;

import java.util.Locale;
import java.util.Map;

public interface OtpTemplateEngine {
    String render(String templateName, Map<String, Object> variables, Locale locale);
}