package ma.noba.identity.infrastructure.crypto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CryptoBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CryptoBootstrap.class);

    @Value("${security.pii.secret-b64}")
    private String secretB64;

    @PostConstruct
    public void init() {
        log.info("Initializing Crypto Bootstrap");
        AesGcmCrypto.initFromBase64(secretB64);
    }

}