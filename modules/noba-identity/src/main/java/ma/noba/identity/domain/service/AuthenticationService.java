package ma.noba.identity.domain.service;

import lombok.RequiredArgsConstructor;
import ma.noba.identity.domain.model.AuthMethod;
import ma.noba.identity.application.AuthToken;
import ma.noba.shared.DomainService;

import java.util.List;


@DomainService
@RequiredArgsConstructor
public class AuthenticationService {

    private final List<Authenticator<?, AuthToken>> authenticators;

    @SuppressWarnings("unchecked")
    public AuthToken authenticate(AuthMethod method, Object request) {
        var a = authenticators.stream().filter(x -> x.supports(method)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported method"));
        return ((Authenticator<Object, AuthToken>) a).authenticate(request);
    }
}