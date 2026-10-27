package ma.noba.app.security;

import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.RevokedTokenStore;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.UserRolePort;
import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import ma.noba.shared.domain.model.OnboardingStatus;
import ma.noba.shared.domain.model.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock UserRolePort userRolePort;
    @Mock RevokedTokenStore revokedTokenStore;
    @Mock UserRepository userRepository;

    private JwtKeyManager keyManager;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        keyManager = new JwtKeyManager(new JwtTokenProperties());
        filter = new JwtAuthenticationFilter(keyManager, userRolePort, revokedTokenStore, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String sign(UUID userId, String subjectOverride, Instant expiration) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .subject(subjectOverride != null ? subjectOverride : userId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issuer("https://api.noba.ma")
                .issueTime(Date.from(Instant.now().minusSeconds(60)))
                .expirationTime(Date.from(expiration))
                .build();
        var jwt = new SignedJWT(keyManager.currentHeader(), claims);
        jwt.sign(new RSASSASigner(keyManager.currentPrivateKey()));
        return jwt.serialize();
    }

    private void invoke(String token) throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
    }

    @Test
    @DisplayName("Valid token authenticates the user")
    void validTokenAuthenticates() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).status(UserStatus.ACTIVE).onboardingStatus(OnboardingStatus.NEW).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRolePort.findRolesByUserId(userId)).thenReturn(Set.of("USER"));

        invoke(sign(userId, null, Instant.now().plusSeconds(300)));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(ma.noba.shared.domain.user.UserPrincipal.class);
        assertThat(((ma.noba.shared.domain.user.UserPrincipal) auth.getPrincipal()).id()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Expired token is rejected even though the signature is valid")
    void expiredTokenIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();

        invoke(sign(userId, null, Instant.now().minusSeconds(60)));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token with a non-UUID subject does not throw and leaves the request anonymous")
    void malformedSubjectIsIgnored() throws Exception {
        invoke(sign(UUID.randomUUID(), "not-a-uuid", Instant.now().plusSeconds(300)));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
