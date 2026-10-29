package ma.noba.notification.application.service;

import lombok.RequiredArgsConstructor;
import ma.noba.notification.application.dto.CampaignStatsView;
import ma.noba.notification.application.dto.CampaignView;
import ma.noba.notification.domain.model.NotificationCampaign;
import ma.noba.notification.domain.port.AdminCampaignRepository;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminAudiencePort;
import ma.noba.shared.domain.port.AdminAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Platform broadcast: resolves an audience, sends one notification per user through the normal
 * pipeline (preferences, quiet hours, digest and dedupe all apply) and keeps a campaign record.
 * Campaigns can be scheduled and cancelled while still pending.
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
    private final NotificationDeliveryRepository deliveries;
    private final AdminAuditPort audit;

    @Transactional(readOnly = true)
    public long preview(String audience) {
        return audiencePort.count(parseAudience(audience));
    }

    @Transactional
    public CampaignView send(UUID actorId, String actorLabel, String title, String body, String audience,
                             Instant scheduledAt) {
        String cleanTitle = cleanTitle(title);
        String cleanBody = cleanBody(body);
        AdminAudiencePort.Audience parsed = parseAudience(audience);
        long total = resolveAudienceSize(parsed);

        if (scheduledAt != null && scheduledAt.isAfter(Instant.now())) {
            NotificationCampaign campaign = campaigns.persist(NotificationCampaign.scheduled(
                    cleanTitle, cleanBody, parsed.name(), (int) total, actorId, scheduledAt));
            audit.record(actorId, actorLabel, "BROADCAST_SCHEDULED", "CAMPAIGN", campaign.getId(),
                    Map.of("audience", parsed.name(), "total", total, "scheduledAt", scheduledAt.toString()));
            return toView(campaign);
        }
        NotificationCampaign campaign = campaigns.persist(
                NotificationCampaign.create(cleanTitle, cleanBody, parsed.name(), (int) total, actorId));
        return deliver(campaign, parsed, actorId, actorLabel);
    }

    /** Sends one scheduled campaign; called per campaign by the scheduler job. */
    @Transactional
    public CampaignView sendScheduled(UUID campaignId) {
        NotificationCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found"));
        if (!campaign.isScheduled()) {
            return toView(campaign);
        }
        campaign.begin();
        campaigns.persist(campaign);
        return deliver(campaign, parseAudience(campaign.getAudience()), campaign.getCreatedBy(), "scheduler");
    }

    @Transactional
    public CampaignView cancel(UUID actorId, String actorLabel, UUID campaignId) {
        NotificationCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found"));
        if (!campaign.isScheduled() && !"SENDING".equals(campaign.getStatus())) {
            throw new IllegalArgumentException("Only pending campaigns can be cancelled");
        }
        campaign.cancel(actorId);
        campaigns.persist(campaign);
        audit.record(actorId, actorLabel, "BROADCAST_CANCELLED", "CAMPAIGN", campaignId,
                Map.of("status", campaign.getStatus()));
        return toView(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignView detail(UUID campaignId) {
        return toView(campaigns.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found")));
    }

    @Transactional(readOnly = true)
    public CampaignStatsView stats(UUID campaignId) {
        NotificationCampaign campaign = campaigns.findById(campaignId)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found"));
        Map<String, Map<String, Long>> byChannel = new LinkedHashMap<>();
        for (Object[] row : deliveries.statsForDedupePrefix("campaign:" + campaignId + ":")) {
            String channel = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();
            byChannel.computeIfAbsent(channel, key -> new LinkedHashMap<>()).merge(status, count, Long::sum);
        }
        return new CampaignStatsView(campaign.getId(), campaign.getStatus(), campaign.getTotalRecipients(),
                campaign.getSentCount(), campaign.getFailedCount(), campaign.getCreatedAt(),
                campaign.getScheduledAt(), byChannel);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CampaignView> recent(int page, int size) {
        Page<NotificationCampaign> result = campaigns.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return PagedResponse.of(
                result.getContent().stream().map(AdminCampaignService::toView).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    /** Ids of scheduled campaigns whose send time has passed (bounded batch). */
    @Transactional(readOnly = true)
    public List<UUID> dueCampaignIds(int limit) {
        return campaigns.findDueScheduled(Instant.now(), PageRequest.of(0, Math.min(Math.max(limit, 1), 50)))
                .stream().map(NotificationCampaign::getId).toList();
    }

    private CampaignView deliver(NotificationCampaign campaign, AdminAudiencePort.Audience parsed,
                                 UUID actorId, String actorLabel) {
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
                            campaign.getTitle(),
                            campaign.getBody(),
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
                Map.of("audience", parsed.name(), "total", campaign.getTotalRecipients(),
                        "sent", sent, "failed", failed));
        return toView(campaign);
    }

    private long resolveAudienceSize(AdminAudiencePort.Audience audience) {
        long total = audiencePort.count(audience);
        if (total == 0) {
            throw new IllegalArgumentException("The selected audience is empty");
        }
        if (total > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("Audience too large (" + total + "); the limit is " + MAX_RECIPIENTS);
        }
        return total;
    }

    private static String cleanTitle(String title) {
        String value = title == null ? "" : title.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (value.length() > 160) {
            throw new IllegalArgumentException("Title must be at most 160 characters");
        }
        return value;
    }

    private static String cleanBody(String body) {
        String value = body == null ? "" : body.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Body is required");
        }
        if (value.length() > 2000) {
            throw new IllegalArgumentException("Body must be at most 2000 characters");
        }
        return value;
    }

    private static CampaignView toView(NotificationCampaign campaign) {
        return new CampaignView(
                campaign.getId(), campaign.getTitle(), campaign.getBody(), campaign.getAudience(),
                campaign.getStatus(), campaign.getTotalRecipients(), campaign.getSentCount(),
                campaign.getFailedCount(), campaign.getCreatedBy(), campaign.getCreatedAt(),
                campaign.getScheduledAt());
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
