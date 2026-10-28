package ma.noba.app.notification;

import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.shared.domain.exception.PushDeliveryException;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import ma.noba.shared.domain.port.PushGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationChannelTest {

    @Mock PushGateway pushGateway;
    @Mock FcmTokenQueryPort tokens;

    private PushNotificationChannel channel;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        channel = new PushNotificationChannel(pushGateway, tokens);
    }

    private Notification notification() {
        Notification n = Notification.create(userId, "test.event", null, 1, "Title", "Body",
                Map.of("k", "v"), null, "en");
        n.setId(UUID.randomUUID());
        return n;
    }

    private NotificationDelivery delivery(Notification n) {
        NotificationDelivery d = NotificationDelivery.queued(n.getId(), NotificationChannel.PUSH, "push", null);
        d.setId(UUID.randomUUID());
        return d;
    }

    @Test
    @DisplayName("Resolves the token and sends through the push gateway")
    void sendsThroughGateway() {
        Notification n = notification();
        NotificationDelivery d = delivery(n);
        when(tokens.findFcmTokenByUserId(userId)).thenReturn(Optional.of("token-1"));

        channel.send(n, d);

        verify(pushGateway).send(eq("token-1"), eq("Title"), eq("Body"), eq(Map.of("k", "v")));
    }

    @Test
    @DisplayName("A user without a token is an invalid recipient")
    void missingTokenIsInvalid() {
        Notification n = notification();
        NotificationDelivery d = delivery(n);
        when(tokens.findFcmTokenByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channel.send(n, d))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(e -> assertThat(((NotificationDeliveryException) e).isInvalidRecipient()).isTrue());
    }

    @Test
    @DisplayName("An invalid FCM token is removed before failing")
    void invalidTokenIsRemoved() {
        Notification n = notification();
        NotificationDelivery d = delivery(n);
        when(tokens.findFcmTokenByUserId(userId)).thenReturn(Optional.of("token-1"));
        doThrow(new PushDeliveryException(true, "UNREGISTERED", null))
                .when(pushGateway).send(any(), any(), any(), any());

        assertThatThrownBy(() -> channel.send(n, d))
                .isInstanceOf(NotificationDeliveryException.class);
        verify(tokens).invalidateToken("token-1");
    }
}
