package ma.noba.reminder;

import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import ma.noba.reminder.infra.ReminderNotificationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderNotificationJobTest {

    @Mock ServiceReminderRepository reminderRepository;
    @Mock NotificationPort notifications;

    private ReminderNotificationJob job;

    @BeforeEach
    void setUp() {
        job = new ReminderNotificationJob(reminderRepository, notifications);
    }

    private ServiceReminder reminder() {
        ServiceReminder reminder = new ServiceReminder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Atlas Barber Club", "Coupe classique", 30);
        reminder.setId(UUID.randomUUID());
        return reminder;
    }

    @Test
    @DisplayName("A due reminder is recorded as a notification and snoozed")
    void sendsDueReminder() {
        ServiceReminder reminder = reminder();
        when(reminderRepository.findByActiveTrueAndNextReminderAtBefore(any())).thenReturn(List.of(reminder));

        job.processDueReminders();

        ArgumentCaptor<NotificationPort.Command> captor = ArgumentCaptor.forClass(NotificationPort.Command.class);
        verify(notifications).send(captor.capture());
        NotificationPort.Command command = captor.getValue();
        assertThat(command.userId()).isEqualTo(reminder.getUserId());
        assertThat(command.type()).isEqualTo("reminder.service");
        assertThat(command.title()).isEqualTo("Service Reminder");
        assertThat(command.body()).contains("Coupe classique");
        assertThat(command.dedupeKey()).startsWith("reminder:" + reminder.getId());
        verify(reminderRepository).save(reminder);
    }

    @Test
    @DisplayName("No due reminders means no notifications")
    void noDueReminders() {
        when(reminderRepository.findByActiveTrueAndNextReminderAtBefore(any())).thenReturn(List.of());

        job.processDueReminders();

        verify(notifications, never()).send(any());
    }
}
