package ma.noba.identity.interfaces.rest;


import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import ma.noba.identity.infrastructure.crypto.JwtKeyManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtKeyManager manager;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> jwks() {
        JWKSet jwkSet = manager.publicJwkSet();
        return ResponseEntity.ok(jwkSet.toJSONObject());
    }
}
