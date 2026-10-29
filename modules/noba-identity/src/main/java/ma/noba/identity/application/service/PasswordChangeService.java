package ma.noba.identity.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.repository.CredentialPasswordRepository;
import ma.noba.identity.domain.service.PasswordHasher;
import ma.noba.shared.domain.port.AdminAuditPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Self-service password change for accounts that have a password credential. */
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final CredentialPasswordRepository credentials;
    private final PasswordHasher hasher;
    private final ObjectProvider<AdminAuditPort> audit;

    @Transactional
    public void change(UUID userId, String actorLabel, String currentPassword, String newPassword) {
        var credential = credentials.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("This account has no password set"));
        if (!hasher.matches(currentPassword, credential.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is incorrect");
        }
        if (hasher.matches(newPassword, credential.getPasswordHash())) {
            throw new IllegalArgumentException("The new password must be different from the current one");
        }
        credential.setPasswordHash(hasher.hash(newPassword));
        credential.setPasswordUpdatedAt(Instant.now());
        credential.setMustChangePassword(false);
        credentials.save(credential);
        AdminAuditPort auditor = audit.getIfAvailable();
        if (auditor != null) {
            auditor.record(userId, actorLabel, "PASSWORD_CHANGED", "USER", userId, Map.of());
        }
    }
}
