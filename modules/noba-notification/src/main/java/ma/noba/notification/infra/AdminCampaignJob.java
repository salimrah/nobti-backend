package ma.noba.notification.infra;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.service.AdminCampaignService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sends scheduled broadcasts when their time arrives (one transaction per campaign). */
@Component
@RequiredArgsConstructor
public class AdminCampaignJob {

    private static final Logger log = LoggerFactory.getLogger(AdminCampaignJob.class);

    private final AdminCampaignService campaigns;

    @Scheduled(fixedDelayString = "${notification.campaign.poll-ms:60000}")
    @SchedulerLock(name = "admin-campaign-dispatch", lockAtMostFor = "15m", lockAtLeastFor = "30s")
    public void run() {
        for (UUID campaignId : campaigns.dueCampaignIds(20)) {
            try {
                var campaign = campaigns.sendScheduled(campaignId);
                log.info("Scheduled campaign {} finished with status {}", campaignId, campaign.status());
            } catch (Exception e) {
                log.warn("Scheduled campaign {} failed: {}", campaignId, e.getMessage());
            }
        }
    }
}
