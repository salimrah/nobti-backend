package ma.noba.identity.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.application.AuthToken;
import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.AuditService;
import ma.noba.identity.domain.service.TokenPort;
import ma.noba.identity.domain.service.TotpService;
import ma.noba.shared.domain.exception.ErrorCode;
import ma.noba.shared.domain.exception.ValidationException;
import ma.noba.shared.domain.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** TOTP enrolment and the second login step. */
@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorController.class);

    private final TotpService totp;
    private final TokenPort tokens;
    private final UserRepository users;
    private final AuditService auditService;

    public record CodeRequest(@NotBlank String code) {
    }

    public record VerifyRequest(@NotBlank String mfaToken, @NotBlank String code) {
    }

    public record SetupResponse(String secret, String otpauthUri) {
    }

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> status(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(Map.of("enabled", totp.isEnabled(user.id())));
    }

    @PostMapping("/setup")
    public ResponseEntity<SetupResponse> setup(@AuthenticationPrincipal UserPrincipal user) {
        String label = user.email() != null ? user.email() : user.phone();
        TotpService.Setup setup = totp.startSetup(user.id(), label);
        return ResponseEntity.ok(new SetupResponse(setup.secret(), setup.otpauthUri()));
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, Boolean>> enable(@AuthenticationPrincipal UserPrincipal user,
                                                       @Valid @RequestBody CodeRequest request) {
        totp.enable(user.id(), request.code());
        auditService.record(user.id(), "MFA_ENABLED", null, null);
        return ResponseEntity.ok(Map.of("enabled", true));
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, Boolean>> disable(@AuthenticationPrincipal UserPrincipal user,
                                                        @Valid @RequestBody CodeRequest request) {
        totp.disable(user.id(), request.code());
        auditService.record(user.id(), "MFA_DISABLED", null, null);
        return ResponseEntity.ok(Map.of("enabled", false));
    }

    /** Second step of login: exchanges a short-lived MFA token + TOTP code for real tokens. */
    @PostMapping("/verify")
    public AuthToken verify(@Valid @RequestBody VerifyRequest request) {
        UUID userId = tokens.verifyMfaToken(request.mfaToken())
                .orElseThrow(() -> new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS,
                        "Invalid or expired verification token"));
        if (!totp.verify(userId, request.code())) {
            auditService.record(userId, "MFA_FAILED", null, null);
            throw new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid verification code");
        }
        User user = users.findById(userId)
                .orElseThrow(() -> new ValidationException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid account"));
        auditService.record(userId, "MFA_VERIFIED", null, null);
        log.info("TOTP second factor verified for user {}", userId);
        return tokens.issueForUser(user, null);
    }
}
