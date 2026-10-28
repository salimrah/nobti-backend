package ma.noba.reminder;

import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import ma.noba.reminder.infra.ReminderNotificationJob;
import ma.noba.shared.domain.exception.PushDeliveryException;
import ma.noba.shared.domain.port.FcmTokenQueryPort;
import ma.noba.shared.domain.port.PushGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationJobTest {

    @Mock ServiceReminderRepository reminderRepository;
    @Mock FcmTokenQueryPort fcmTokens;
    @Mock PushGateway pushGateway;

    private ReminderNotificationJob job;

    @BeforeEach
    void setUp() {
        job = new ReminderNotificationJob(reminderRepository, fcmTokens, pushGateway);
    }

    private ServiceReminder reminder() {
        ServiceReminder reminder = new ServiceReminder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Atlas Barber Club", "Coupe classique", 30);
        reminder.setId(UUID.randomUUID());
        return reminder;
    }

    @Test
    @DisplayName("A due reminder is sent through the push gateway and snoozed")
    void sendsDueReminder() {
        ServiceReminder reminder = reminder();
        when(reminderRepository.findByActiveTrueAndNextReminderAtBefore(any())).thenReturn(List.of(reminder));
        when(fcmTokens.findFcmTokenByUserId(reminder.getUserId())).thenReturn(Optional.of("token-12345678"));

        job.processDueReminders();

        verify(pushGateway).send(eq("token-12345678"), eq("Service Reminder"),
                contains("Coupe classique"), anyMap());
        verify(reminderRepository, atLeastOnce()).save(reminder);
    }

    @Test
    @DisplayName("A reminder without a device token is deactivated")
    void missingTokenDeactivatesReminder() {
        ServiceReminder reminder = reminder();
        when(reminderRepository.findByActiveTrueAndNextReminderAtBefore(any())).thenReturn(List.of(reminder));
        when(fcmTokens.findFcmTokenByUserId(reminder.getUserId())).thenReturn(Optional.empty());

        job.processDueReminders();

        verify(pushGateway, never()).send(any(), any(), any(), anyMap());
        assertThat(reminder.isActive()).isFalse();
        verify(reminderRepository, atLeastOnce()).save(reminder);
    }

    @Test
    @DisplayName("An invalid token reported by the gateway is removed")
    void invalidTokenIsRemoved() {
        ServiceReminder reminder = reminder();
        when(reminderRepository.findByActiveTrueAndNextReminderAtBefore(any())).thenReturn(List.of(reminder));
        when(fcmTokens.findFcmTokenByUserId(reminder.getUserId())).thenReturn(Optional.of("token-12345678"));
        doThrow(new PushDeliveryException(true, "UNREGISTERED", null))
                .when(pushGateway).send(any(), any(), any(), anyMap());

        job.processDueReminders();

        verify(fcmTokens).invalidateToken("token-12345678");
    }
}
