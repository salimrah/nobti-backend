package ma.noba.identity.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.application.*;
import ma.noba.identity.domain.exception.TooManyRequestsException;
import ma.noba.identity.domain.model.AuthToken;
import ma.noba.identity.domain.service.*;
import ma.noba.identity.infrastructure.JwtLogoutHandler;
import ma.noba.identity.domain.model.AuthMethod;
import ma.noba.identity.domain.model.SocialProvider;
import ma.noba.identity.infrastructure.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final OtpService otpService;
    private final AuthenticationService authService;
    private final TokenService tokenService;
    private final SignupService signupService;
    private final PasswordResetService passwordResetService;
    private final AccountLinkingService accountLinkingService;
    private final JwtLogoutHandler jwtLogoutHandler;
    private final RateLimiterService rateLimiter;

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    @PostMapping("/otp/start")
    public ChallengeResponse startOtp(@Valid @RequestBody StartOtpRequest req, @RequestHeader(value = "Idempotency-Key", required = false) String idem, Locale locale) {
        log.info("Starting otp challenge");
        if (idem != null && !UUID_PATTERN.matcher(idem).matches()) {
            throw new IllegalArgumentException("Invalid Idempotency-Key format — must be a UUID");
        }
        String id = otpService.startChallenge(req.target(), req.channel(), req.purpose(), locale, idem);
        return new ChallengeResponse(id);
    }

    @PostMapping("/otp/verify")
    public AuthToken verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return authService.authenticate(AuthMethod.PHONE_OTP, req);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest req) {
        var result = signupService.signup(
                new SignupService.SignupData(req.email(), req.password(), req.firstName(), req.lastName()));
        return ResponseEntity.created(URI.create("/auth/session"))
                .body(new SignupResponse(result.userId(), result.email(), result.emailVerified()));
    }

    @PostMapping("/login")
    public AuthToken login(@Valid @RequestBody PasswordAuthRequest req) {
        return authService.authenticate(AuthMethod.PASSWORD, req);
    }

    @PostMapping("/oauth/verify")
    public AuthToken oauthVerify(@Valid @RequestBody SocialAuthRequest req) {
        return authService.authenticate(AuthMethod.SOCIAL, req);
    }

    @PostMapping("/token/refresh")
    public AuthToken refresh(@Valid @RequestBody RefreshTokenRequest req) {
        if (!rateLimiter.tryConsume("rl:token:refresh:" + req.refreshToken().hashCode(), 1)) {
            throw new TooManyRequestsException("Rate limit");
        }
        return tokenService.refresh(req.refreshToken());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        if (!rateLimiter.tryConsume("rl:forgot:pw:" + req.email().toLowerCase(java.util.Locale.ROOT), 1)) {
            throw new TooManyRequestsException("Rate limit");
        }
        passwordResetService.requestReset(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        signupService.verifyEmail(req.email(), req.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        if (!rateLimiter.tryConsume("rl:resend:verify:" + req.email().toLowerCase(java.util.Locale.ROOT), 1)) {
            throw new TooManyRequestsException("Rate limit");
        }
        signupService.resendVerification(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest req) {
        if (req.accessToken() != null) {
            tokenService.revoke(req.accessToken());
        }
        if (req.refreshToken() != null) {
            var hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(req.refreshToken());
            tokenService.revokeRefreshToken(hash);
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> logoutCurrent(@AuthenticationPrincipal org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof String jwt) {
            tokenService.revoke(jwt);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/accounts/linked")
    public ResponseEntity<List<String>> getLinkedAccounts(@AuthenticationPrincipal org.springframework.security.core.Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(accountLinkingService.getLinkedProviders(userId));
    }

    @PostMapping("/accounts/link")
    public ResponseEntity<List<String>> linkAccount(@AuthenticationPrincipal org.springframework.security.core.Authentication auth,
                                                     @Valid @RequestBody LinkAccountRequest req) {
        UUID userId = extractUserId(auth);
        var providers = accountLinkingService.linkAccount(userId, req.provider(), req.idToken(), req.accessToken());
        return ResponseEntity.ok(providers);
    }

    @PostMapping("/accounts/unlink")
    public ResponseEntity<List<String>> unlinkAccount(@AuthenticationPrincipal org.springframework.security.core.Authentication auth,
                                                       @Valid @RequestBody UnlinkAccountRequest req) {
        UUID userId = extractUserId(auth);
        var providers = accountLinkingService.unlinkAccount(userId, req.provider());
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/hello")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("noba-backend is running");
    }

    // Item 6: List active sessions
    @GetMapping("/sessions")
    public ResponseEntity<List<TokenService.ActiveSession>> listSessions(
            @AuthenticationPrincipal org.springframework.security.core.Authentication auth) {
        UUID userId = extractUserId(auth);
        return ResponseEntity.ok(tokenService.getActiveSessions(userId));
    }

    // Item 7: Revoke all sessions (logout all devices)
    @PostMapping("/sessions/revoke-all")
    public ResponseEntity<Void> revokeAllSessions(
            @AuthenticationPrincipal org.springframework.security.core.Authentication auth) {
        UUID userId = extractUserId(auth);
        tokenService.revokeAllUserTokens(userId);
        return ResponseEntity.noContent().build();
    }

    // Item 5: Revoke a specific session
    @DeleteMapping("/sessions/{tokenId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal org.springframework.security.core.Authentication auth) {
        UUID userId = extractUserId(auth);
        boolean revoked = tokenService.revokeSession(tokenId, userId);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private UUID extractUserId(org.springframework.security.core.Authentication auth) {
        if (auth == null) throw new IllegalArgumentException("Not authenticated");
        if (auth.getPrincipal() instanceof ma.noba.shared.domain.user.UserPrincipal up) {
            return up.id();
        }
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalArgumentException("Unknown principal type: " + auth.getPrincipal().getClass());
    }

    public record SignupResponse(UUID userId, String email, boolean emailVerified) {}
    public record LogoutRequest(String accessToken, String refreshToken) {}
    public record UnlinkAccountRequest(SocialProvider provider) {}
}
