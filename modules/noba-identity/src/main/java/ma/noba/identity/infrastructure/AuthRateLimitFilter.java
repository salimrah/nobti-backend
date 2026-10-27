package ma.noba.identity.infrastructure;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import ma.noba.identity.domain.service.RateLimiterService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthRateLimitFilter implements Filter {
    private final RateLimiterService rateLimiter;

    public AuthRateLimitFilter(RateLimiterService rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getRequestURI().startsWith("/api/v1/auth")) {
            String ip = req.getRemoteAddr();
            if (!rateLimiter.tryConsume("rl:ip:" + ip, 1)) {
                ((jakarta.servlet.http.HttpServletResponse) response).setStatus(429);
                response.getWriter().write("{\"detail\":\"Too many requests\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
