package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationEvent;
import ma.noba.notification.domain.port.NotificationEventRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationEventRepository
        extends JpaRepository<NotificationEvent, Long>, NotificationEventRepository {

    @Override
    default void record(NotificationEvent event) {
        save(event);
    }
}
