package ma.noba.identity;

import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

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

    @Test
    @DisplayName("Configured RSA key is used as-is and is not rotated or regenerated")
    void usesConfiguredKeyAndSkipsRotation() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        JwtTokenProperties props = new JwtTokenProperties();
        props.jwt.rsaPrivateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        props.jwt.rsaPublicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        JwtKeyManager keyManager = new JwtKeyManager(props);

        assertThat(keyManager.allPublicKeys()).hasSize(1);
        assertThat(keyManager.currentPrivateKey()).isEqualTo(pair.getPrivate());
        assertThat(keyManager.currentPublicKey()).isEqualTo(pair.getPublic());

        String kid = keyManager.getCurrentKid();
        keyManager.rotate(); // no-op: key is supplied by configuration

        assertThat(keyManager.getCurrentKid()).isEqualTo(kid);
        assertThat(keyManager.allPublicKeys()).hasSize(1);
        assertThat(keyManager.currentPrivateKey()).isEqualTo(pair.getPrivate());
    }

    @Test
    @DisplayName("PEM-wrapped configured keys are accepted")
    void acceptsPemEncodedKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();

        JwtTokenProperties props = new JwtTokenProperties();
        props.jwt.rsaPrivateKey = pem("PRIVATE KEY", pair.getPrivate().getEncoded());
        props.jwt.rsaPublicKey = pem("PUBLIC KEY", pair.getPublic().getEncoded());

        JwtKeyManager keyManager = new JwtKeyManager(props);

        assertThat(keyManager.currentPrivateKey()).isEqualTo(pair.getPrivate());
        assertThat(keyManager.allPublicKeys()).hasSize(1);
    }

    private static String pem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----";
    }
}
