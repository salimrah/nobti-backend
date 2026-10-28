package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCampaignRepository {

    NotificationCampaign persist(NotificationCampaign campaign);

    Page<NotificationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
