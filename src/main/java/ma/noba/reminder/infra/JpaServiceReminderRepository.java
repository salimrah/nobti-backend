package ma.noba.reminder.infra;

import ma.noba.reminder.domain.model.ServiceReminder;
import ma.noba.reminder.domain.port.ServiceReminderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaServiceReminderRepository extends JpaRepository<ServiceReminder, UUID>, ServiceReminderRepository {
    List<ServiceReminder> findByUserIdOrderByNextReminderAtAsc(UUID userId);
    List<ServiceReminder> findByActiveTrueAndNextReminderAtBefore(Instant now);

    @Override
    default List<ServiceReminder> findByUserId(UUID userId) {
        return findByUserIdOrderByNextReminderAtAsc(userId);
    }
}
