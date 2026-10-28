package ma.noba.app.notification;

import ma.noba.identity.domain.service.SmsGateway;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationRecipientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsNotificationChannelTest {

    @Mock SmsGateway smsGateway;
    @Mock NotificationRecipientPort recipients;

    private SmsNotificationChannel channel;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        channel = new SmsNotificationChannel(smsGateway, recipients);
    }

    @Test
    @DisplayName("Sends to the user's phone with title and body")
    void sendsSms() {
        Notification n = Notification.create(userId, "test", null, 1, "Title", "Body", null, null, "en");
        n.setId(UUID.randomUUID());
        NotificationDelivery d = NotificationDelivery.queued(n.getId(), NotificationChannel.SMS, "sms", null);
        d.setId(UUID.randomUUID());
        when(recipients.phone(userId)).thenReturn(Optional.of("+212700000010"));

        channel.send(n, d);

        verify(smsGateway).sendSms("+212700000010", "Title: Body");
    }
}
