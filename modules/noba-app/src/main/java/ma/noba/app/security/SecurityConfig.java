package ma.noba.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final ProblemDetailsAuthEntryPoint problemDetailsAuthEntryPoint;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8081,http://localhost:19006}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          ProblemDetailsAuthEntryPoint problemDetailsAuthEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.problemDetailsAuthEntryPoint = problemDetailsAuthEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .xssProtection(xss -> xss
                                .headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .frameOptions(frame -> frame
                                .deny())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'none'; style-src 'none'")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Return 401 (not the default 403) for unauthenticated requests to protected endpoints.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problemDetailsAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/sessions/**").authenticated()
                        .requestMatchers("/api/v1/auth/password").authenticated()
                        // The second login step runs before tokens exist.
                        .requestMatchers("/api/v1/auth/2fa/verify").permitAll()
                        .requestMatchers("/api/v1/auth/2fa", "/api/v1/auth/2fa/**").authenticated()
                        .requestMatchers("/api/v1/providers/self/**").authenticated()
                        .requestMatchers("/api/v1/providers/documents/*/file").authenticated()
                        .requestMatchers("/api/v1/storage/**").authenticated()
                        .requestMatchers("/api/v1/providers/*/wait-insights").authenticated()
                        // State-changing queue endpoints must be authenticated (they dereference the
                        // caller's principal). Public queue reads (status) remain permitted below.
                        .requestMatchers(
                                "/api/v1/queues/providers/*/members",
                                "/api/v1/queues/providers/*/join",
                                "/api/v1/queues/providers/*/leave",
                                "/api/v1/queues/providers/*/my-position",
                                "/api/v1/queues/providers/*/waitlist",
                                "/api/v1/queues/providers/self/**",
                                "/api/v1/queues/my/**",
                                "/api/v1/queues/waitlist/**"
                        ).authenticated()
                        // Authenticated provider actions that live under the otherwise-public
                        // /providers/** tree.
                        .requestMatchers(
                                "/api/v1/providers/*/ratings",
                                "/api/v1/providers/*/favorite",
                                "/api/v1/providers/favorites",
                                "/api/v1/providers/*/appointments"
                        ).authenticated()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/app/**",
                                // Public uploads served by the storage layer (private files live
                                // under a separate prefix and are streamed through the API).
                                "/media/**",
                                "/api/v1/providers/**",
                                "/api/v1/stripe/webhook",
                                // OpenAPI spec + Swagger UI (used to generate client types).
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // STOMP/SockJS handshake endpoint (also exposes /ws/websocket for clients).
                                "/ws",
                                "/ws/**",
                                "/.well-known/**",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/actuator/**").authenticated()
                        .requestMatchers("/api/v1/queues/**").permitAll()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/users/{userId}").authenticated()
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
