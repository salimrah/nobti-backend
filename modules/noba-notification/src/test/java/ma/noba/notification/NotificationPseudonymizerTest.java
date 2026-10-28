package ma.noba.notification;

import ma.noba.notification.application.service.NotificationPseudonymizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationPseudonymizerTest {

    private final NotificationPseudonymizer pseudonymizer = new NotificationPseudonymizer("test-secret", "");

    @Test
    @DisplayName("The same user always maps to the same pseudonym")
    void stable() {
        UUID userId = UUID.randomUUID();

        assertThat(pseudonymizer.apply(userId))
                .isEqualTo(pseudonymizer.apply(userId))
                .startsWith("u_")
                .hasSize(34)
                .doesNotContain(userId.toString());
    }

    @Test
    @DisplayName("Different users get different pseudonyms")
    void distinct() {
        assertThat(pseudonymizer.apply(UUID.randomUUID()))
                .isNotEqualTo(pseudonymizer.apply(UUID.randomUUID()));
    }

    @Test
    @DisplayName("A different key produces a different pseudonym for the same user")
    void keyScoped() {
        UUID userId = UUID.randomUUID();
        assertThat(new NotificationPseudonymizer("other", "").apply(userId))
                .isNotEqualTo(pseudonymizer.apply(userId));
    }

    @Test
    @DisplayName("A missing key fails fast instead of exporting raw ids")
    void requiresKey() {
        assertThatThrownBy(() -> new NotificationPseudonymizer(" ", ""))
                .isInstanceOf(IllegalStateException.class);
    }
}
