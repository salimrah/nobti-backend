package ma.noba.contract;

import ma.noba.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void helloEndpointReturns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/hello", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("noba-backend is running");
    }

    @Test
    void listProvidersReturns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/providers", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void listCategoriesReturns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/providers/categories", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void queueStatusForNonexistentProviderReturns200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/queues/providers/00000000-0000-0000-0000-000000000000/status", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
