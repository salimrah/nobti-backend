package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationCampaign;
import ma.noba.notification.domain.port.AdminCampaignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAdminCampaignRepository
        extends JpaRepository<NotificationCampaign, UUID>, AdminCampaignRepository {

    @Override
    default NotificationCampaign persist(NotificationCampaign campaign) {
        return save(campaign);
    }

    @Override
    Page<NotificationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
