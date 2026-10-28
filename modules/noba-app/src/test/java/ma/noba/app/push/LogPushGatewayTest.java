package ma.noba.app.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class LogPushGatewayTest {

    @Test
    @DisplayName("Log gateway never throws (dev / no-credentials mode)")
    void logsWithoutThrowing() {
        LogPushGateway gateway = new LogPushGateway();

        assertThatCode(() -> gateway.send("token-12345678", "Title", "Body", Map.of("type", "test")))
                .doesNotThrowAnyException();
    }
}
