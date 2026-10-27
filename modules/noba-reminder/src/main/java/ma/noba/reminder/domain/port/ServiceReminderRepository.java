package ma.noba.reminder.domain.port;

import ma.noba.reminder.domain.model.ServiceReminder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceReminderRepository {
    ServiceReminder save(ServiceReminder reminder);
    Optional<ServiceReminder> findById(UUID id);
    List<ServiceReminder> findByUserId(UUID userId);
    List<ServiceReminder> findByActiveTrueAndNextReminderAtBefore(Instant now);
    void delete(ServiceReminder reminder);
    void deleteById(UUID id);
}
