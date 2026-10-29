package ma.noba.notification.infra;

import ma.noba.notification.domain.model.NotificationCampaign;
import ma.noba.notification.domain.port.AdminCampaignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaAdminCampaignRepository
        extends JpaRepository<NotificationCampaign, UUID>, AdminCampaignRepository {

    @Override
    default NotificationCampaign persist(NotificationCampaign campaign) {
        return save(campaign);
    }

    @Override
    Page<NotificationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @Query("select c from NotificationCampaign c where c.status = 'SCHEDULED' " +
            "and c.scheduledAt is not null and c.scheduledAt <= :now order by c.scheduledAt asc")
    List<NotificationCampaign> findDueScheduled(@Param("now") Instant now, Pageable pageable);
}
