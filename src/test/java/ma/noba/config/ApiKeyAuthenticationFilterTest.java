package ma.noba.config;

import ma.noba.shared.domain.model.ApiKey;
import ma.noba.shared.domain.port.service.ApiKeyRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiKeyAuthenticationFilterTest {

    private static final String RAW_KEY = "noba_test_key";
    private static final String KEY_HASH = DigestUtils.sha256Hex(RAW_KEY);

    private ApiKeyRepository repository;
    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        repository = mock(ApiKeyRepository.class);
        filter = new ApiKeyAuthenticationFilter(repository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ApiKey key(String permissions) {
        ApiKey k = new ApiKey();
        k.setKeyHash(KEY_HASH);
        k.setOwner("integration");
        k.setPermissions(permissions);
        return k;
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/api/v1/providers");
        req.addHeader("X-API-Key", RAW_KEY);
        return req;
    }

    @Test
    @DisplayName("Valid read key authenticates and continues the chain")
    void validReadKey() throws Exception {
        when(repository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(key("read")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("GET"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(authorities()).contains("ROLE_API", "SCOPE_read");
    }

    @Test
    @DisplayName("A read-only key is rejected with 403 on a write")
    void readOnlyKeyCannotWrite() throws Exception {
        when(repository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(key("read")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("POST"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("A write key is allowed to mutate state")
    void writeKeyCanWrite() throws Exception {
        when(repository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(key("read,write")));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("POST"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authorities()).contains("SCOPE_write");
    }

    @Test
    @DisplayName("Unknown key yields 401")
    void unknownKey() throws Exception {
        when(repository.findByKeyHash(KEY_HASH)).thenReturn(Optional.empty());

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("No API key header leaves the request untouched")
    void noHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/providers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Set<String> authorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }
}
