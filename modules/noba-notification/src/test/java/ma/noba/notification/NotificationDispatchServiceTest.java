package ma.noba.notification;

import ma.noba.notification.application.service.NotificationDispatchService;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationStatus;
import ma.noba.notification.domain.port.NotificationChannelPort;
import ma.noba.notification.domain.port.NotificationDeliveryException;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import ma.noba.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock NotificationRepository notifications;
    @Mock NotificationDeliveryRepository deliveries;
    @Mock NotificationEventRepository events;
    @Mock NotificationChannelPort pushChannel;

    private NotificationDispatchService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NotificationDispatchService(notifications, deliveries, events, List.of(pushChannel));
        lenient().when(deliveries.saveDelivery(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(pushChannel.channel()).thenReturn(NotificationChannel.PUSH);
    }

    private Notification notification() {
        Notification n = Notification.create(userId, "test.event", null, 1, "Title", "Body",
                Map.of("k", "v"), "dedupe", "en");
        n.setId(UUID.randomUUID());
        n.setStatus(NotificationStatus.QUEUED);
        return n;
    }

    private NotificationDelivery delivery(UUID notificationId) {
        NotificationDelivery d = NotificationDelivery.queued(notificationId, NotificationChannel.PUSH, "push", null);
        d.setId(UUID.randomUUID());
        return d;
    }

    @Test
    @DisplayName("A due push delivery is sent and the notification becomes SENT")
    void sendsDueDelivery() {
        Notification n = notification();
        NotificationDelivery d = delivery(n.getId());
        when(deliveries.findDue(any(), any())).thenReturn(List.of(d));
        when(notifications.findById(n.getId())).thenReturn(Optional.of(n));
        when(deliveries.findByNotificationId(n.getId())).thenReturn(List.of(d));

        service.dispatchDue();

        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(d.getAttempt()).isEqualTo(1);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(pushChannel).send(n, d);
    }

    @Test
    @DisplayName("An invalid recipient is suppressed and not retried")
    void invalidRecipientIsSuppressed() {
        Notification n = notification();
        NotificationDelivery d = delivery(n.getId());
        when(deliveries.findDue(any(), any())).thenReturn(List.of(d));
        when(notifications.findById(n.getId())).thenReturn(Optional.of(n));
        when(deliveries.findByNotificationId(n.getId())).thenReturn(List.of(d));
        doThrow(new NotificationDeliveryException(true, false, "UNREGISTERED", null))
                .when(pushChannel).send(n, d);

        service.dispatchDue();

        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.SUPPRESSED);
        assertThat(d.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("A retryable failure is scheduled with a next attempt")
    void retryableFailureIsScheduled() {
        Notification n = notification();
        NotificationDelivery d = delivery(n.getId());
        when(deliveries.findDue(any(), any())).thenReturn(List.of(d));
        when(notifications.findById(n.getId())).thenReturn(Optional.of(n));
        when(deliveries.findByNotificationId(n.getId())).thenReturn(List.of(d));
        doThrow(new NotificationDeliveryException(false, true, "timeout", null))
                .when(pushChannel).send(n, d);

        service.dispatchDue();

        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d.getNextAttemptAt()).isNotNull();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.QUEUED);
    }

    @Test
    @DisplayName("A delivery with no adapter fails without retry")
    void missingAdapterFails() {
        NotificationDispatchService withoutAdapters =
                new NotificationDispatchService(notifications, deliveries, events, List.of());
        Notification n = notification();
        NotificationDelivery d = delivery(n.getId());
        when(deliveries.findDue(any(), any())).thenReturn(List.of(d));
        when(notifications.findById(n.getId())).thenReturn(Optional.of(n));
        when(deliveries.findByNotificationId(n.getId())).thenReturn(List.of(d));

        withoutAdapters.dispatchDue();

        assertThat(d.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d.getErrorCode()).isEqualTo("NO_ADAPTER");
        assertThat(d.getNextAttemptAt()).isNull();
    }
}
