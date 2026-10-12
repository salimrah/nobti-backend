package ma.noba.identity;

import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyManagerTest {

    @Test
    @DisplayName("Old signing keys are retired so a rotated key stops validating new tokens")
    void retainsOnlyRecentKeys() {
        JwtKeyManager keyManager = new JwtKeyManager(new JwtTokenProperties());

        assertThat(keyManager.allPublicKeys()).hasSize(1);

        keyManager.rotate();
        assertThat(keyManager.allPublicKeys()).hasSize(2);

        keyManager.rotate();
        keyManager.rotate();
        // Bounded: current + one previous key only.
        assertThat(keyManager.allPublicKeys()).hasSize(2);
    }
}
