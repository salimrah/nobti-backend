package ma.noba.notification;

import ma.noba.notification.application.service.NotificationDeliveryAdminService;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryAdminServiceTest {

    @Mock NotificationDeliveryRepository deliveries;
    @Mock NotificationEventRepository events;

    private NotificationDeliveryAdminService service;

    @BeforeEach
    void setUp() {
        service = new NotificationDeliveryAdminService(deliveries, events);
    }

    private NotificationDelivery delivery(DeliveryStatus status) {
        NotificationDelivery delivery = NotificationDelivery.queued(
                UUID.randomUUID(), NotificationChannel.PUSH, "push", null);
        delivery.setId(UUID.randomUUID());
        delivery.setStatus(status);
        delivery.setErrorCode("DELIVERY_FAILED");
        delivery.setErrorMessage("boom");
        delivery.setNextAttemptAt(null);
        return delivery;
    }

    @Test
    @DisplayName("A failed delivery is requeued and its error cleared")
    void retriesFailedDelivery() {
        NotificationDelivery failed = delivery(DeliveryStatus.FAILED);
        when(deliveries.findById(failed.getId())).thenReturn(Optional.of(failed));

        var result = service.retry(failed.getId());

        assertThat(result.status()).isEqualTo("QUEUED");
        assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.QUEUED);
        assertThat(failed.getNextAttemptAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(failed.getErrorCode()).isNull();
        verify(deliveries).saveDelivery(failed);
        verify(events).record(any());
    }

    @Test
    @DisplayName("Only failed deliveries can be retried")
    void refusesNonFailedDelivery() {
        NotificationDelivery sent = delivery(DeliveryStatus.SENT);
        when(deliveries.findById(sent.getId())).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> service.retry(sent.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failed");

        verify(deliveries, never()).saveDelivery(any());
    }

    @Test
    @DisplayName("An unknown delivery is reported as not found")
    void unknownDelivery() {
        UUID id = UUID.randomUUID();
        when(deliveries.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retry(id))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
