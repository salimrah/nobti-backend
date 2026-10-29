package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.UserTotpSecret;
import ma.noba.identity.domain.repository.UserTotpSecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    @Mock UserTotpSecretRepository secrets;

    private TotpService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TotpService(secrets);
        lenient().when(secrets.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Setup generates a Base32 secret and an otpauth URI")
    void setupPayload() {
        when(secrets.findById(userId)).thenReturn(Optional.empty());

        TotpService.Setup setup = service.startSetup(userId, "ops@noba.ma");

        assertThat(setup.secret()).matches("[A-Z2-7]{16,}");
        assertThat(setup.otpauthUri())
                .startsWith("otpauth://totp/")
                .contains("secret=" + setup.secret())
                .contains("issuer=");
    }

    @Test
    @DisplayName("Enrolment confirms the secret and the same step cannot be replayed")
    void enableAndReplayProtection() {
        UserTotpSecret record = pendingSecret("JBSWY3DPEHPK3PXP");
        when(secrets.findById(userId)).thenReturn(Optional.of(record));

        service.enable(userId, code("JBSWY3DPEHPK3PXP", 0));

        assertThat(record.isEnabled()).isTrue();
        assertThat(record.getConfirmedAt()).isNotNull();
        assertThat(service.isEnabled(userId)).isTrue();
        // Same time step → replay rejected.
        assertThat(service.verify(userId, code("JBSWY3DPEHPK3PXP", 0))).isFalse();
        // Next step → accepted.
        assertThat(service.verify(userId, code("JBSWY3DPEHPK3PXP", 1))).isTrue();
    }

    @Test
    @DisplayName("A wrong code is rejected and enrolment requires a code")
    void invalidCodes() {
        UserTotpSecret record = pendingSecret("JBSWY3DPEHPK3PXP");
        when(secrets.findById(userId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.enable(userId, "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid");

        record.setEnabled(false);
        assertThat(service.verify(userId, code("JBSWY3DPEHPK3PXP", 0))).isFalse();
    }

    @Test
    @DisplayName("Disabling requires a valid code and removes the secret")
    void disableRequiresCode() {
        UserTotpSecret record = pendingSecret("JBSWY3DPEHPK3PXP");
        record.setEnabled(true);
        when(secrets.findById(userId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.disable(userId, "000000"))
                .isInstanceOf(IllegalArgumentException.class);

        service.disable(userId, code("JBSWY3DPEHPK3PXP", 0));
    }

    private static UserTotpSecret pendingSecret(String secret) {
        UserTotpSecret record = new UserTotpSecret();
        record.setUserId(UUID.randomUUID());
        record.setSecret(secret);
        return record;
    }

    /** Independent RFC 6238 implementation used to cross-check the service. */
    private static String code(String secret, int stepOffset) {
        try {
            byte[] key = TotpService.base32Decode(secret);
            long step = Instant.now().getEpochSecond() / 30 + stepOffset;
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
