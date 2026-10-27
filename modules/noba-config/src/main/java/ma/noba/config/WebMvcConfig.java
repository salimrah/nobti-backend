package ma.noba.config;

import ma.noba.shared.infra.idempotency.IdempotencyInterceptor;
import ma.noba.shared.infra.ratelimit.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final IdempotencyInterceptor idempotencyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final DeprecationHeaderInterceptor deprecationHeaderInterceptor;
    private final ApiVersionInterceptor apiVersionInterceptor;

    public WebMvcConfig(@Autowired(required = false) IdempotencyInterceptor idempotencyInterceptor,
                        @Autowired(required = false) RateLimitInterceptor rateLimitInterceptor,
                        DeprecationHeaderInterceptor deprecationHeaderInterceptor,
                        ApiVersionInterceptor apiVersionInterceptor) {
        this.idempotencyInterceptor = idempotencyInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.deprecationHeaderInterceptor = deprecationHeaderInterceptor;
        this.apiVersionInterceptor = apiVersionInterceptor;
    }

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        var reg = new FilterRegistrationBean<ForwardedHeaderFilter>();
        reg.setFilter(new ForwardedHeaderFilter());
        reg.setOrder(0);
        return reg;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitInterceptor != null) {
            registry.addInterceptor(rateLimitInterceptor)
                    .addPathPatterns("/api/v1/**");
        }

        if (idempotencyInterceptor != null) {
            registry.addInterceptor(idempotencyInterceptor)
                    .addPathPatterns("/api/v1/**")
                    .excludePathPatterns("/api/v1/auth/**");
        }

        registry.addInterceptor(deprecationHeaderInterceptor)
                .addPathPatterns("/api/v1/**");

        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**");
    }
}
