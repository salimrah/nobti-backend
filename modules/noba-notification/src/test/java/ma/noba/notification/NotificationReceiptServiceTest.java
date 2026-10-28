package ma.noba.notification;

import ma.noba.notification.application.service.NotificationReceiptService;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.model.NotificationEventType;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationReceiptServiceTest {

    @Mock NotificationDeliveryRepository deliveries;
    @Mock NotificationEventRepository events;

    private NotificationReceiptService service;

    @BeforeEach
    void setUp() {
        service = new NotificationReceiptService(deliveries, events);
    }

    private NotificationDelivery sentDelivery() {
        NotificationDelivery delivery = NotificationDelivery.queued(
                UUID.randomUUID(), NotificationChannel.PUSH, "push", null);
        delivery.setId(UUID.randomUUID());
        delivery.markSending();
        delivery.markSent("msg-1", 12);
        return delivery;
    }

    @Test
    @DisplayName("A delivered receipt stamps deliveredAt and records the event")
    void deliveredReceipt() {
        NotificationDelivery delivery = sentDelivery();
        when(deliveries.findByProviderMessageId("msg-1")).thenReturn(Optional.of(delivery));

        var result = service.record(new NotificationReceiptService.Receipt(
                null, "msg-1", NotificationReceiptService.ReceiptStatus.DELIVERED, null, null, null));

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getDeliveredAt()).isNotNull();
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(events).record(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(NotificationEventType.DELIVERED);
        verify(deliveries).saveDelivery(delivery);
    }

    @Test
    @DisplayName("A failure receipt is terminal and kept for the admin view")
    void failedReceipt() {
        NotificationDelivery delivery = sentDelivery();
        when(deliveries.findById(delivery.getId())).thenReturn(Optional.of(delivery));

        service.record(new NotificationReceiptService.Receipt(
                delivery.getId(), null, NotificationReceiptService.ReceiptStatus.FAILED,
                null, "BOUNCE", "mailbox full"));

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getErrorCode()).isEqualTo("BOUNCE");
        assertThat(delivery.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("Re-delivering the same receipt is idempotent")
    void duplicateReceiptIsIgnored() {
        NotificationDelivery delivery = sentDelivery();
        delivery.markDelivered();
        when(deliveries.findById(delivery.getId())).thenReturn(Optional.of(delivery));

        service.record(new NotificationReceiptService.Receipt(
                delivery.getId(), null, NotificationReceiptService.ReceiptStatus.DELIVERED,
                null, null, null));

        verify(events, never()).record(any());
    }

    @Test
    @DisplayName("An unknown delivery id returns empty")
    void unknownDelivery() {
        when(deliveries.findByProviderMessageId("missing")).thenReturn(Optional.empty());

        assertThat(service.record(new NotificationReceiptService.Receipt(
                null, "missing", NotificationReceiptService.ReceiptStatus.SENT, null, null, null)))
                .isEmpty();
    }
}
