package ma.noba.identity.infrastructure.hash;

import ma.noba.identity.domain.service.HashService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

@Component
public class Sha256HashService implements HashService {

    @Override
    public String hash(String input) {
        return DigestUtils.sha256Hex(input);
    }
}
