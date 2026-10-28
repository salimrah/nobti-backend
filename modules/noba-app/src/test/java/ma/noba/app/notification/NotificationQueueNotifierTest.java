package ma.noba.app.notification;

import ma.noba.notification.domain.port.NotificationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationQueueNotifierTest {

    @Mock NotificationPort notifications;
    @InjectMocks NotificationQueueNotifier notifier;

    private final UUID clientId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @Test
    @DisplayName("Turn-start is recorded as an inbox + push notification")
    void turnStart() {
        notifier.notifyTurnStart(clientId, providerId, 3);

        ArgumentCaptor<NotificationPort.Command> captor = ArgumentCaptor.forClass(NotificationPort.Command.class);
        verify(notifications).send(captor.capture());
        var command = captor.getValue();
        assertThat(command.userId()).isEqualTo(clientId);
        assertThat(command.type()).isEqualTo("queue.turn_start");
        assertThat(command.channels()).containsExactly(NotificationPort.Channel.PUSH);
        assertThat(command.dedupeKey()).isEqualTo("queue.turn:" + providerId + ":" + clientId + ":3");
        assertThat(command.body()).contains("your turn");
    }

    @Test
    @DisplayName("Estimated wait and waitlist-open are recorded with their own dedupe keys")
    void waitAndWaitlist() {
        notifier.notifyEstimatedWait(clientId, providerId, 15);
        notifier.notifyWaitlistOpen(clientId, providerId);

        ArgumentCaptor<NotificationPort.Command> captor = ArgumentCaptor.forClass(NotificationPort.Command.class);
        verify(notifications, times(2)).send(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationPort.Command::type)
                .containsExactly("queue.estimated_wait", "queue.waitlist_open");
        assertThat(captor.getAllValues().get(0).body()).contains("15");
    }
}
