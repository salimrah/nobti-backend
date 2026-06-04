package ma.noba.shared.infra.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RedisIdempotencyStore idempotencyStore;
    private final ObjectMapper objectMapper;

    public IdempotencyInterceptor(RedisIdempotencyStore idempotencyStore, ObjectMapper objectMapper) {
        this.idempotencyStore = idempotencyStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (!isIdempotentMethod(method)) {
            return true;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }

        RedisIdempotencyStore.CachedResponse cached = idempotencyStore.get(idempotencyKey);
        if (cached != null) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            response.setStatus(cached.statusCode());
            response.setContentType("application/json");
            for (Map.Entry<String, String> header : cached.headers().entrySet()) {
                response.setHeader(header.getKey(), header.getValue());
            }
            response.setHeader("X-Idempotency-Cached", "true");
            response.getOutputStream().write(cached.body());
            return false;
        }

        request.setAttribute(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        Object keyAttr = request.getAttribute(IDEMPOTENCY_KEY_HEADER);
        if (keyAttr == null) {
            return;
        }

        String idempotencyKey = (String) keyAttr;
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] body = wrapper.getContentAsByteArray();
            Map<String, String> headers = new HashMap<>();
            for (String name : wrapper.getHeaderNames()) {
                headers.put(name, wrapper.getHeader(name));
            }

            idempotencyStore.save(idempotencyKey, new RedisIdempotencyStore.CachedResponse(
                    response.getStatus(), headers, body
            ));
            wrapper.copyBodyToResponse();
        }
    }

    private boolean isIdempotentMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }
}
