package ma.noba;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@Tag("docker")
@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.redis.url=redis://localhost:6379",
        "spring.datasource.url=jdbc:postgresql://localhost:5432/noba",
        "security.pii.secret-b64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest"
})
class NobaBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
