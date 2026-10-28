package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationEvent;

public interface NotificationEventRepository {

    void record(NotificationEvent event);
}
