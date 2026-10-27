package ma.noba.identity.infrastructure.crypto;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JWSHeader;
import lombok.Getter;
import ma.noba.identity.infrastructure.config.JwtTokenProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class JwtKeyManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtKeyManager.class);

    private final Map<String, RSAKey> keys = new ConcurrentHashMap<>();
    private final java.util.Deque<String> keyOrder = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private final java.nio.file.Path keyStore;

    /** How many signing keys are kept valid at once (current + previous, to survive rotation). */
    private static final int RETAINED_KEYS = 2;

    @Getter
    private volatile String currentKid;

    public JwtKeyManager(JwtTokenProperties props) {
        String configured = props.jwt != null ? props.jwt.keyStorePath : null;
        this.keyStore = (configured != null && !configured.isBlank())
                ? java.nio.file.Path.of(configured)
                : null;
        // Reuse persisted keys so a restart does not invalidate every issued token.
        if (keyStore == null || !loadPersistedKeys()) {
            rotate();
        }
    }

    private java.nio.file.Path currentKidFile() {
        return keyStore.resolveSibling(keyStore.getFileName() + ".current");
    }

    private boolean loadPersistedKeys() {
        if (keyStore == null) {
            return false;
        }
        try {
            if (!java.nio.file.Files.exists(keyStore)) {
                return false;
            }
            String json = java.nio.file.Files.readString(keyStore);
            JWKSet set = JWKSet.parse(json);
            for (JWK jwk : set.getKeys()) {
                if (jwk instanceof RSAKey rsa) {
                    keys.put(rsa.getKeyID(), rsa);
                    keyOrder.addLast(rsa.getKeyID());
                }
            }
            if (keys.isEmpty()) {
                return false;
            }
            // Restore which key is "current" (used for signing new tokens); validation uses all keys.
            java.nio.file.Path kidFile = currentKidFile();
            if (java.nio.file.Files.exists(kidFile)) {
                String persistedKid = java.nio.file.Files.readString(kidFile).trim();
                if (keys.containsKey(persistedKid)) {
                    currentKid = persistedKid;
                }
            }
            if (currentKid == null) {
                currentKid = keyOrder.peekLast();
            }
            log.info("Loaded {} persisted JWT signing key(s)", keys.size());
            return true;
        } catch (Exception e) {
            log.warn("Failed to load persisted JWT keys from {}: {}", keyStore, e.getMessage());
            return false;
        }
    }

    private void persistKeys() {
        if (keyStore == null) {
            return;
        }
        try {
            if (keyStore.getParent() != null) {
                java.nio.file.Files.createDirectories(keyStore.getParent());
            }
            // Includes private material — keep this file out of source control (defaults to the temp dir).
            // Serialize with private material (JWKSet.toString() emits public keys only).
            java.nio.file.Files.writeString(keyStore,
                    new JWKSet(new java.util.ArrayList<>(keys.values())).toJSONObject(false).toString());
            java.nio.file.Files.writeString(currentKidFile(), currentKid == null ? "" : currentKid);
            try {
                java.nio.file.Files.setPosixFilePermissions(keyStore,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (Exception ignored) {
                // non-POSIX filesystem
            }
        } catch (Exception e) {
            log.warn("Failed to persist JWT keys to {}: {}", keyStore, e.getMessage());
        }
    }

    // Rotate every N hours, where N comes from security.jwt.key-rotation-hours. The initial delay
    // equals the period so a restart does not immediately retire the key that signed live tokens.
    @Scheduled(fixedDelayString = "${security.jwt.key-rotation-hours:24}",
               initialDelayString = "${security.jwt.key-rotation-hours:24}",
               timeUnit = TimeUnit.HOURS)
    public void rotate() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);

            KeyPair kp = gen.generateKeyPair();
            RSAKey jwk = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();

            String kid = jwk.getKeyID();
            keys.put(kid, jwk);
            currentKid = kid;

            // Retire older keys so a rotated (e.g. compromised) key stops validating new tokens,
            // and the key map does not grow without bound.
            keyOrder.addLast(kid);
            while (keyOrder.size() > RETAINED_KEYS) {
                String retired = keyOrder.pollFirst();
                if (retired != null) {
                    keys.remove(retired);
                }
            }
            persistKeys();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to rotate JWT key", e);
        }
    }

    public RSAPublicKey currentPublicKey() {
        try {
            return keys.get(currentKid).toRSAPublicKey();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public RSAPrivateKey currentPrivateKey() {
        try {
            return keys.get(currentKid).toRSAPrivateKey();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<RSAPublicKey> allPublicKeys() {
        return keys.values().stream().map(key -> {
            try {
                return key.toRSAPublicKey();
            } catch (JOSEException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public JWSHeader currentHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(currentKid).build();
    }

    public JWKSet publicJwkSet() {
        return new JWKSet((JWK) keys.values().stream().map(RSAKey::toPublicJWK).toList());
    }
}