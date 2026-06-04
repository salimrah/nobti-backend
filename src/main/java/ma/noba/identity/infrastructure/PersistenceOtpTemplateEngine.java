package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.model.OtpTemplate;
import ma.noba.identity.domain.repository.OtpTemplateRepository;
import ma.noba.identity.domain.service.OtpTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "otp.template.provider", havingValue = "persistence")
public class PersistenceOtpTemplateEngine implements OtpTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(PersistenceOtpTemplateEngine.class);
    private static final String CACHE_NAME = "otpTemplates";

    private final CacheManager cacheManager;
    private final OtpTemplateRepository repository;

    public PersistenceOtpTemplateEngine(CacheManager cacheManager, OtpTemplateRepository repository) {
        this.cacheManager = cacheManager;
        this.repository = repository;
    }

    @Override
    public String render(String templateName, Map<String, Object> variables, Locale locale) {
        String raw = resolveRaw(templateName, locale);
        return MessageSourceOtpTemplateEngine.apply(raw, variables);
    }

    private String resolveRaw(String templateName, Locale locale) {
        String cacheKey = localeKey(locale) + ":" + templateName;
        Cache cache = cacheManager.getCache(CACHE_NAME);

        if (cache != null) {
            String cached = cache.get(cacheKey, String.class);
            if (cached != null) {
                log.debug("OTP template cache hit: {}", cacheKey);
                return cached;
            }
        }

        Optional<String> dbResult = findRaw(templateName, locale);
        if (dbResult.isPresent()) {
            log.debug("OTP template DB hit, caching: {}", cacheKey);
            if (cache != null) cache.put(cacheKey, dbResult.get());
            return dbResult.get();
        }

        throw new IllegalArgumentException("No template found for name='" + templateName + "' locale='" + locale + "'");
    }

    private Optional<String> findRaw(String templateName, Locale locale) {
        return repository.findByLocaleAndName(locale.toLanguageTag(), templateName)
                .map(OtpTemplate::getContent)
                .or(() -> repository.findByLocaleAndName(locale.getLanguage(), templateName)
                        .map(OtpTemplate::getContent));
    }

    private static String localeKey(Locale locale) {
        return locale != null ? locale.toLanguageTag() : "en";
    }
}
