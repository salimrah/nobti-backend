package ma.noba.identity.interfaces.rest;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.port.JwksPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwksPort jwksPort;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> jwks() {
        return ResponseEntity.ok(jwksPort.publicJwks());
    }
}
