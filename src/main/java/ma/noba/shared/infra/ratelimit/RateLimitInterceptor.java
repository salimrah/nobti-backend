package ma.noba.shared.infra.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.noba.shared.domain.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitInterceptor(ApiRateLimiter rateLimiter, ObjectMapper objectMapper,
                                RateLimitProperties rateLimitProperties) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        String method = request.getMethod();

        var limits = rateLimitProperties.getLimits();
        RateLimitConfig matchedConfig = findMatchingConfig(path, limits);
        if (matchedConfig == null || !matchedConfig.enabled()) {
            return true;
        }

        String userId = null;
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (auth.getPrincipal() instanceof UserDetails ud) {
                userId = ud.getUsername();
            } else {
                userId = auth.getPrincipal().toString();
            }
        }

        String key = userId != null
                ? userId + ":" + method + ":" + path
                : clientIp + ":" + method + ":" + path;

        if (!rateLimiter.isAllowed(key, matchedConfig.maxRequests(), matchedConfig.windowSeconds())) {
            long resetTime = rateLimiter.getResetTimeSeconds(key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-RateLimit-Limit", String.valueOf(matchedConfig.maxRequests()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
            response.setHeader("Retry-After", String.valueOf(resetTime));

            ErrorResponse error = ErrorResponse.of("RATE_LIMIT_EXCEEDED",
                    "Rate limit exceeded. Try again in " + resetTime + " seconds.", path);

            objectMapper.writeValue(response.getOutputStream(), error);
            return false;
        }

        long remaining = rateLimiter.getRemainingRequests(key, matchedConfig.maxRequests());
        long resetTime = rateLimiter.getResetTimeSeconds(key);
        response.setHeader("X-RateLimit-Limit", String.valueOf(matchedConfig.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));

        return true;
    }

    private RateLimitConfig findMatchingConfig(String path, Map<String, RateLimitConfig> limits) {
        for (Map.Entry<String, RateLimitConfig> entry : limits.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.equals("default")) {
                continue;
            }
            if (path.startsWith(pattern)) {
                return entry.getValue();
            }
        }
        return limits.get("default");
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
