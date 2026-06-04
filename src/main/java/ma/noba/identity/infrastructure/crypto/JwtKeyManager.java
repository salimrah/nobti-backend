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

    private final Map<String, RSAKey> keys = new ConcurrentHashMap<>();

    @Getter
    private volatile String currentKid;

    public JwtKeyManager(JwtTokenProperties props) {
        // generate initial key
        rotate();
    }

    // Rotate every N hours, where N comes from security.jwt.key-rotation-hours
    @Scheduled(fixedDelayString = "${security.jwt.key-rotation-hours:24}", timeUnit = TimeUnit.HOURS, initialDelay = 0)
    public void rotate() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);

            KeyPair kp = gen.generateKeyPair();
            RSAKey jwk = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();

            keys.put(jwk.getKeyID(), jwk);
            currentKid = jwk.getKeyID();

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