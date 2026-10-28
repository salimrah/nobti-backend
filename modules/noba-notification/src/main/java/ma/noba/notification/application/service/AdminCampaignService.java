package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.CampaignView;
import ma.noba.notification.domain.model.NotificationCampaign;
import ma.noba.notification.domain.port.AdminCampaignRepository;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminAudiencePort;
import ma.noba.shared.domain.port.AdminAuditPort;
import ma.noba.notification.domain.port.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Platform broadcast: resolves an audience, sends one notification per user through the normal
 * pipeline (preferences, quiet hours, digest and dedupe all apply) and keeps a campaign record.
 */
@Service
@RequiredArgsConstructor
public class AdminCampaignService {

    private static final Logger log = LoggerFactory.getLogger(AdminCampaignService.class);
    private static final int MAX_RECIPIENTS = 5_000;
    private static final int PAGE_SIZE = 200;

    private final AdminCampaignRepository campaigns;
    private final AdminAudiencePort audiencePort;
    private final NotificationPort notifications;
    private final AdminAuditPort audit;

    @Transactional(readOnly = true)
    public long preview(String audience) {
        return audiencePort.count(parseAudience(audience));
    }

    @Transactional
    public CampaignView send(UUID actorId, String actorLabel, String title, String body, String audience) {
        String cleanTitle = title == null ? "" : title.trim();
        String cleanBody = body == null ? "" : body.trim();
        if (cleanTitle.isEmpty() || cleanBody.isEmpty()) {
            throw new IllegalArgumentException("Title and body are required");
        }
        if (cleanTitle.length() > 160) {
            throw new IllegalArgumentException("Title must be at most 160 characters");
        }
        if (cleanBody.length() > 2000) {
            throw new IllegalArgumentException("Body must be at most 2000 characters");
        }
        AdminAudiencePort.Audience parsed = parseAudience(audience);
        long total = audiencePort.count(parsed);
        if (total == 0) {
            throw new IllegalArgumentException("The selected audience is empty");
        }
        if (total > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("Audience too large (" + total + "); the limit is " + MAX_RECIPIENTS);
        }

        NotificationCampaign campaign = campaigns.persist(
                NotificationCampaign.create(cleanTitle, cleanBody, parsed.name(), (int) total, actorId));

        int sent = 0;
        int failed = 0;
        int page = 0;
        while (true) {
            List<UUID> recipients = audiencePort.page(parsed, page, PAGE_SIZE);
            if (recipients.isEmpty()) {
                break;
            }
            for (UUID userId : recipients) {
                try {
                    notifications.send(new NotificationPort.Command(
                            userId,
                            "campaign.broadcast",
                            NotificationPort.Category.MARKETING,
                            1,
                            cleanTitle,
                            cleanBody,
                            Map.of("type", "broadcast", "campaignId", campaign.getId().toString()),
                            "campaign:" + campaign.getId() + ":" + userId,
                            null,
                            Set.of(NotificationPort.Channel.PUSH),
                            null,
                            null));
                    sent++;
                } catch (Exception e) {
                    failed++;
                    log.warn("Broadcast {} failed for user {}: {}", campaign.getId(), userId, e.getMessage());
                }
            }
            page++;
        }

        campaign.complete(sent, failed);
        campaigns.persist(campaign);
        audit.record(actorId, actorLabel, "BROADCAST_SENT", "CAMPAIGN", campaign.getId(),
                Map.of("audience", parsed.name(), "total", total, "sent", sent, "failed", failed));
        return toView(campaign);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CampaignView> recent(int page, int size) {
        Page<NotificationCampaign> result = campaigns.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PagedResponse.of(
                result.getContent().stream().map(AdminCampaignService::toView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static CampaignView toView(NotificationCampaign campaign) {
        return new CampaignView(
                campaign.getId(), campaign.getTitle(), campaign.getBody(), campaign.getAudience(),
                campaign.getStatus(), campaign.getTotalRecipients(), campaign.getSentCount(),
                campaign.getFailedCount(), campaign.getCreatedBy(), campaign.getCreatedAt());
    }

    private static AdminAudiencePort.Audience parseAudience(String value) {
        try {
            return AdminAudiencePort.Audience.valueOf(
                    (value == null ? "ALL" : value.trim()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown audience: " + value);
        }
    }
}
