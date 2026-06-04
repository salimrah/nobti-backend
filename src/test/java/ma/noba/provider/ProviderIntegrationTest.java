package ma.noba.provider;

import ma.noba.identity.application.PasswordAuthRequest;
import ma.noba.identity.application.SignupRequest;
import ma.noba.provider.application.dto.AddServiceRequest;
import ma.noba.provider.application.dto.CreateProviderRequest;
import ma.noba.provider.application.dto.NearbyProviderResponse;
import ma.noba.provider.application.dto.ProviderResponse;
import ma.noba.provider.application.dto.ProviderServiceResponse;
import ma.noba.shared.AbstractIntegrationTest;
import ma.noba.shared.application.dto.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String accessToken;
    private UUID providerId;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    @BeforeEach
    void setUp() {
        signupAndLogin();
        createProvider();
    }

    private void signupAndLogin() {
        String email = "provider-test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        SignupRequest signupRequest = new SignupRequest(email, "ProviderTest123!", "Provider", "Tester");
        restTemplate.postForEntity(baseUrl() + "/auth/signup", signupRequest, Map.class);

        PasswordAuthRequest loginRequest = new PasswordAuthRequest(email, "ProviderTest123!", null);
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(baseUrl() + "/auth/login", loginRequest, Map.class);
        assertThat(loginResponse.getBody()).isNotNull();
        this.accessToken = (String) loginResponse.getBody().get("accessToken");
    }

    private void createProvider() {
        CreateProviderRequest request = new CreateProviderRequest(
                "Integration Test Provider",
                "A provider for integration testing",
                "Hair Salon",
                "456 Test Ave",
                33.5731,
                -7.5898,
                null,
                null
        );

        HttpEntity<CreateProviderRequest> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<ProviderResponse> response = restTemplate.postForEntity(baseUrl() + "/providers/self", entity, ProviderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.providerId = response.getBody().id();
    }

    @Test
    @DisplayName("After provider creation, user should have both USER and PROVIDER roles")
    void providerCreation_assignsProviderRole() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                baseUrl() + "/users/me", HttpMethod.GET, entity, Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        Set<String> roles = (Set<String>) meResponse.getBody().get("roles");
        assertThat(roles).contains("USER", "PROVIDER");
    }

    @Test
    @DisplayName("Should get all active providers (paginated)")
    void getAllProviders() {
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(
                baseUrl() + "/providers",
                PagedResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.content()).isNotNull();
    }

    @Test
    @DisplayName("Should get provider by ID")
    void getProviderById() {
        ResponseEntity<ProviderResponse> response = restTemplate.getForEntity(
                baseUrl() + "/providers/" + providerId,
                ProviderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProviderResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Integration Test Provider");
    }

    @Test
    @DisplayName("Should search providers by category")
    void searchByCategory() {
        ResponseEntity<PagedResponse> response = restTemplate.getForEntity(
                baseUrl() + "/providers/search?category=Hair+Salon",
                PagedResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should get my provider profile")
    void getMyProvider() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ResponseEntity<ProviderResponse> response = restTemplate.exchange(
                baseUrl() + "/providers/self",
                HttpMethod.GET,
                entity,
                ProviderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Integration Test Provider");
    }

    @Test
    @DisplayName("Should update provider profile")
    void updateProvider() {
        CreateProviderRequest request = new CreateProviderRequest(
                "Updated Provider Name",
                "Updated description",
                "Barber Shop",
                "789 New St",
                33.5731,
                -7.5898,
                null,
                null
        );

        HttpEntity<CreateProviderRequest> entity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<ProviderResponse> response = restTemplate.exchange(
                baseUrl() + "/providers/self",
                HttpMethod.PUT,
                entity,
                ProviderResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Updated Provider Name");
    }

    @Test
    @DisplayName("Should add service to provider")
    void addService() {
        AddServiceRequest request = new AddServiceRequest("Haircut", Duration.ofMinutes(30), BigDecimal.valueOf(50.0), null);

        HttpEntity<AddServiceRequest> entity = new HttpEntity<>(request, authHeaders());

        ResponseEntity<ProviderServiceResponse> response = restTemplate.postForEntity(
                baseUrl() + "/providers/self/services",
                entity,
                ProviderServiceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProviderServiceResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.name()).isEqualTo("Haircut");
    }

    @Test
    @DisplayName("Should get my services")
    void getMyServices() {
        addService();

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());

        ParameterizedTypeReference<List<ProviderServiceResponse>> typeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<ProviderServiceResponse>> response = restTemplate.exchange(
                baseUrl() + "/providers/self/services",
                HttpMethod.GET,
                entity,
                typeRef
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    @DisplayName("Should get nearby providers")
    void getNearbyProviders() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl() + "/providers/nearby?lat=33.5731&lon=-7.5898&radiusKm=10",
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should get service categories")
    void getCategories() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                baseUrl() + "/providers/self/categories",
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
