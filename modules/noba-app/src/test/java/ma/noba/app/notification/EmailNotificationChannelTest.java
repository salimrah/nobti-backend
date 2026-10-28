package ma.noba.app.notification;

import ma.noba.identity.domain.service.EmailSender;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.notification.domain.port.NotificationRecipientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock EmailSender emailSender;
    @Mock NotificationRecipientPort recipients;

    private EmailNotificationChannel channel;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(emailSender, recipients);
    }

    private Notification notification() {
        Notification n = Notification.create(userId, "test", null, 1, "Title", "Body", null, null, "en");
        n.setId(UUID.randomUUID());
        return n;
    }

    private NotificationDelivery delivery(Notification n) {
        NotificationDelivery d = NotificationDelivery.queued(n.getId(), NotificationChannel.EMAIL, "email", null);
        d.setId(UUID.randomUUID());
        return d;
    }

    @Test
    @DisplayName("Sends to the user's email address")
    void sendsEmail() {
        Notification n = notification();
        when(recipients.email(userId)).thenReturn(Optional.of("user@example.com"));

        channel.send(n, delivery(n));

        verify(emailSender).send("user@example.com", "Title", "Body");
    }

    @Test
    @DisplayName("A user without an email is an invalid recipient")
    void missingEmail() {
        Notification n = notification();
        when(recipients.email(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channel.send(n, delivery(n)))
                .isInstanceOf(NotificationDeliveryException.class)
                .satisfies(e -> assertThat(((NotificationDeliveryException) e).isInvalidRecipient()).isTrue());
    }
}
