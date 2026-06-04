package ma.noba.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DeprecationHeaderInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        if (handler instanceof HandlerMethod handlerMethod) {
            DeprecatedApi deprecatedApi = handlerMethod.getMethodAnnotation(DeprecatedApi.class);
            if (deprecatedApi != null) {
                response.setHeader("Deprecation", "true");
                String sunset = deprecatedApi.sunset();
                if (!sunset.isEmpty()) {
                    response.setHeader("Sunset", sunset);
                }
                String migration = deprecatedApi.migration();
                if (!migration.isEmpty()) {
                    response.setHeader("X-API-Migration", migration);
                }
            }
        }
    }
}
