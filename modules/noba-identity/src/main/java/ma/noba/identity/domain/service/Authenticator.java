package ma.noba.identity.domain.service;

import ma.noba.identity.domain.model.AuthMethod;

public interface Authenticator<RQ, RS> {
    boolean supports(AuthMethod method);
    RS authenticate(RQ request);
}
