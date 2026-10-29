package ma.noba.notification.domain.port;

import ma.noba.notification.domain.model.NotificationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminCampaignRepository {

    NotificationCampaign persist(NotificationCampaign campaign);

    Page<NotificationCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<NotificationCampaign> findById(UUID id);

    List<NotificationCampaign> findDueScheduled(Instant now, Pageable pageable);
}
