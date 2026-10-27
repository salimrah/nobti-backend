package ma.noba.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.RevokedTokenStore;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.UserRolePort;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import ma.noba.shared.domain.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtKeyManager keyManager;
    private final UserRolePort userRolePort;
    private final RevokedTokenStore revokedTokenStore;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtKeyManager keyManager, UserRolePort userRolePort,
                                   RevokedTokenStore revokedTokenStore, UserRepository userRepository) {
        this.keyManager = keyManager;
        this.userRolePort = userRolePort;
        this.revokedTokenStore = revokedTokenStore;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            boolean verified = keyManager.allPublicKeys().stream()
                    .anyMatch(publicKey -> {
                        try {
                            return signedJWT.verify(new RSASSAVerifier(publicKey));
                        } catch (JOSEException e) {
                            return false;
                        }
                    });
            if (!verified) {
                filterChain.doFilter(request, response);
                return;
            }

            var claims = signedJWT.getJWTClaimsSet();

            // Reject expired tokens: signature verification alone does not validate `exp`.
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.toInstant().isBefore(java.time.Instant.now())) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String userId = claims.getSubject();
            if (userId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String jti = claims.getJWTID();
            if (jti != null && revokedTokenStore.isRevoked(jti)) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID uid = UUID.fromString(userId);
            Optional<User> userOpt = userRepository.findById(uid);
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                log.warn("Rejecting token for inactive or nonexistent user: {}", userId);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            MDC.put("userId", userId);

            Set<String> roles = userRolePort.findRolesByUserId(uid);
            Set<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toSet());

            var auth = new UsernamePasswordAuthenticationToken(
                    new UserPrincipal(uid, null, null, roles),
                    null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (ParseException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
        }
    }
}
