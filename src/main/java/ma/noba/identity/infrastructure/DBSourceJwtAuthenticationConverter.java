package ma.noba.identity.infrastructure;

import ma.noba.identity.domain.model.User;
import ma.noba.identity.domain.port.RevokedTokenStore;
import ma.noba.identity.domain.repository.UserRepository;
import ma.noba.identity.domain.service.UserRolePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DBSourceJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(DBSourceJwtAuthenticationConverter.class);
    private final UserRepository userRepository;
    private final UserRolePort userRolePort;
    private final RevokedTokenStore revokedTokenStore;

    public DBSourceJwtAuthenticationConverter(UserRepository userRepository, UserRolePort userRolePort, RevokedTokenStore revokedTokenStore) {
        this.userRepository = userRepository;
        this.userRolePort = userRolePort;
        this.revokedTokenStore = revokedTokenStore;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String jti = jwt.getId();

        if (revokedTokenStore.isRevoked(jti)) {
            throw new BadCredentialsException("Token revoked");
        }

        String userId = jwt.getSubject();

        log.debug("userId = {}", userId);
        log.debug("jti = {}", jti);

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadCredentialsException("User is disabled");
        }

        Set<String> roles = userRolePort.findRolesByUserId(user.getId());
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, userId);
    }
}
