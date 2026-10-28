package ma.noba.notification;

import ma.noba.notification.application.dto.CreateTemplateRequest;
import ma.noba.notification.application.dto.TemplateResponse;
import ma.noba.notification.application.service.NotificationDigestService;
import ma.noba.notification.application.service.NotificationTemplateAdminService;
import ma.noba.notification.application.service.NotificationReceiptService;
import ma.noba.notification.domain.model.DigestFrequency;
import ma.noba.notification.domain.model.Notification;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.model.NotificationDelivery;
import ma.noba.notification.domain.model.NotificationSetting;
import ma.noba.notification.domain.model.NotificationTemplate;
import ma.noba.notification.domain.port.NotificationAnalyticsRepository;
import ma.noba.notification.domain.port.NotificationDeliveryRepository;
import ma.noba.notification.domain.port.NotificationDigestEntryRepository;
import ma.noba.notification.domain.port.NotificationPort;
import ma.noba.notification.domain.port.NotificationRepository;
import ma.noba.notification.domain.port.NotificationSettingRepository;
import ma.noba.notification.domain.port.NotificationTemplateRepository;
import ma.noba.notification.infra.JpaNotificationTemplateRepository;
import ma.noba.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the notification module against real Postgres: entity mappings, the template lookup,
 * digest batching, delivery receipts and the analytics aggregates.
 */
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired NotificationPort notificationPort;
    @Autowired NotificationRepository notifications;
    @Autowired NotificationDeliveryRepository deliveries;
    @Autowired NotificationDigestEntryRepository digestEntries;
    @Autowired NotificationSettingRepository settings;
    @Autowired NotificationTemplateRepository templates;
    @Autowired JpaNotificationTemplateRepository templateWriter;
    @Autowired NotificationDigestService digestService;
    @Autowired NotificationReceiptService receipts;
    @Autowired NotificationAnalyticsRepository analytics;
    @Autowired NotificationTemplateAdminService templateAdmin;
    @Autowired PlatformTransactionManager transactions;

    private static Instant utc(int month, int day, int hour) {
        return ZonedDateTime.of(2026, month, day, hour, 0, 0, 0, ZoneOffset.UTC).toInstant();
    }

    /** A moment just after tomorrow's digest hour, so entries created now fall inside the window. */
    private static Instant afterTomorrowsDigest() {
        return ZonedDateTime.now(ZoneOffset.UTC).plusDays(1)
                .withHour(9).withMinute(5).withSecond(0).withNano(0).toInstant();
    }

    @Test
    @DisplayName("Sending from an after-commit listener persists the notification")
    void sendFromAfterCommitListener() {
        UUID userId = UUID.randomUUID();
        new TransactionTemplate(transactions).executeWithoutResult(status ->
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        notificationPort.send(new NotificationPort.Command(
                                userId, "post.commit.test", NotificationPort.Category.TRANSACTIONAL, 1,
                                "After commit", "Body", Map.of(), "after-commit:" + userId,
                                "en", Set.of(NotificationPort.Channel.IN_APP), null, null));
                    }
                }));

        assertThat(notifications.findByDedupeKey("after-commit:" + userId)).isPresent();
    }

    @Test
    @DisplayName("Copy is rendered from the seeded template for the requested locale")
    void rendersTemplate() {
        templateWriter.save(NotificationTemplate.of("test.turn_start", "fr", 1,
                "C'est votre tour !", "Présentez-vous dans {{minutes}} minutes."));

        UUID userId = UUID.randomUUID();
        UUID id = notificationPort.send(new NotificationPort.Command(
                userId, "test.turn_start", NotificationPort.Category.TRANSACTIONAL, 1,
                "It's your turn!", "Fallback body", Map.of("minutes", 5),
                "test-template:" + userId, "fr", Set.of(NotificationPort.Channel.IN_APP), null, null));

        Notification saved = notifications.findById(id).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("C'est votre tour !");
        assertThat(saved.getBody()).isEqualTo("Présentez-vous dans 5 minutes.");
        assertThat(saved.getTemplateKey()).isEqualTo("test.turn_start");
        assertThat(saved.getTemplateVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Publishing a new version switches the copy the renderer resolves")
    void templateVersioning() {
        TemplateResponse v1 = templateAdmin.create(new CreateTemplateRequest(
                "versioned.copy", "en", "Version one", "First body", true));
        assertThat(v1.version()).isEqualTo(1);

        UUID userId = UUID.randomUUID();
        UUID first = notificationPort.send(new NotificationPort.Command(
                userId, "versioned.copy", NotificationPort.Category.TRANSACTIONAL, 1,
                "fallback", "fallback", Map.of(), "version-1:" + userId,
                "en", Set.of(NotificationPort.Channel.IN_APP), null, null));
        assertThat(notifications.findById(first).orElseThrow().getTitle()).isEqualTo("Version one");

        TemplateResponse v2 = templateAdmin.create(new CreateTemplateRequest(
                "versioned.copy", "en", "Version two", "Second body", true));
        assertThat(v2.version()).isEqualTo(2);

        UUID second = notificationPort.send(new NotificationPort.Command(
                userId, "versioned.copy", NotificationPort.Category.TRANSACTIONAL, 1,
                "fallback", "fallback", Map.of(), "version-2:" + userId,
                "en", Set.of(NotificationPort.Channel.IN_APP), null, null));
        assertThat(notifications.findById(second).orElseThrow().getTitle()).isEqualTo("Version two");

        // Rolling back to v1 takes effect immediately.
        templateAdmin.setActive(v1.id(), true);
        UUID third = notificationPort.send(new NotificationPort.Command(
                userId, "versioned.copy", NotificationPort.Category.TRANSACTIONAL, 1,
                "fallback", "fallback", Map.of(), "version-3:" + userId,
                "en", Set.of(NotificationPort.Channel.IN_APP), null, null));
        assertThat(notifications.findById(third).orElseThrow().getTitle()).isEqualTo("Version one");

        assertThat(templates.findActive("versioned.copy", "en").orElseThrow().getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("A marketing notification is held for the digest and flushed into one summary")
    void digestBatching() {
        UUID userId = UUID.randomUUID();
        NotificationSetting setting = NotificationSetting.defaults(userId);
        setting.update(null, null, "UTC", "DAILY");
        settings.saveSetting(setting);

        UUID first = notificationPort.send(new NotificationPort.Command(
                userId, "promo.one", NotificationPort.Category.MARKETING, 1,
                "Promo one", "First offer", Map.of(), "digest-test-1:" + userId,
                "en", Set.of(NotificationPort.Channel.PUSH), null, null));
        notificationPort.send(new NotificationPort.Command(
                userId, "promo.two", NotificationPort.Category.MARKETING, 1,
                "Promo two", "Second offer", Map.of(), "digest-test-2:" + userId,
                "en", Set.of(NotificationPort.Channel.PUSH), null, null));

        // Held: the inbox item exists but no push delivery was queued.
        assertThat(digestEntries.findPendingByUserId(userId)).hasSize(2);
        assertThat(deliveries.findByNotificationId(first))
                .extracting(NotificationDelivery::getChannel)
                .containsExactly(NotificationChannel.IN_APP);

        // Nothing is sent before the window closes.
        assertThat(digestService.flush(userId, Instant.now().minusSeconds(86_400))).isFalse();

        assertThat(digestService.flush(userId, afterTomorrowsDigest())).isTrue();

        assertThat(digestEntries.findPendingByUserId(userId)).isEmpty();
        List<Notification> summaries = notifications
                .findByUserIdOrderByCreatedAtDesc(userId, org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent().stream()
                .filter(n -> n.getType().equals("digest.summary"))
                .toList();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getBody()).contains("2 new updates");
        assertThat(summaries.get(0).getCategory()).isEqualTo(NotificationCategory.SYSTEM);
    }

    @Test
    @DisplayName("A delivery receipt marks the delivery delivered")
    void deliveryReceipt() {
        UUID userId = UUID.randomUUID();
        UUID id = notificationPort.send(new NotificationPort.Command(
                userId, "receipt.test", NotificationPort.Category.TRANSACTIONAL, 1,
                "Receipt", "Body", Map.of(), "receipt:" + userId,
                "en", Set.of(NotificationPort.Channel.PUSH), null, null));

        NotificationDelivery push = deliveries.findByNotificationId(id).stream()
                .filter(d -> d.getChannel() == NotificationChannel.PUSH)
                .findFirst().orElseThrow();

        var result = receipts.record(new NotificationReceiptService.Receipt(
                push.getId(), null, NotificationReceiptService.ReceiptStatus.DELIVERED,
                Instant.now(), null, null));

        assertThat(result).isPresent();
        NotificationDelivery reloaded = deliveries.findById(push.getId()).orElseThrow();
        assertThat(reloaded.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("Analytics aggregate notifications and deliveries in the range")
    void analyticsAggregates() {
        UUID userId = UUID.randomUUID();
        notificationPort.send(new NotificationPort.Command(
                userId, "analytics.test", NotificationPort.Category.TRANSACTIONAL, 1,
                "Analytics", "Body", Map.of(), "analytics:" + userId,
                "en", Set.of(NotificationPort.Channel.PUSH), null, null));

        Instant to = Instant.now().plusSeconds(60);
        Instant from = to.minusSeconds(3600);

        assertThat(analytics.countTotal(from, to)).isGreaterThanOrEqualTo(1);
        assertThat(analytics.countByCategory(from, to))
                .anyMatch(c -> c.category() == NotificationCategory.TRANSACTIONAL && c.count() >= 1);
        assertThat(analytics.countByChannelAndStatus(from, to))
                .anyMatch(c -> c.channel() == NotificationChannel.IN_APP);
        assertThat(analytics.countByType(from, to))
                .anyMatch(t -> t.type().equals("analytics.test"));
    }

    @Test
    @DisplayName("Turning digests off releases the held notifications as pushes")
    void releasesWhenDigestDisabled() {
        UUID userId = UUID.randomUUID();
        NotificationSetting setting = NotificationSetting.defaults(userId);
        setting.update(null, null, "UTC", "DAILY");
        settings.saveSetting(setting);

        UUID id = notificationPort.send(new NotificationPort.Command(
                userId, "promo.release", NotificationPort.Category.MARKETING, 1,
                "Release me", "Body", Map.of(), "release:" + userId,
                "en", Set.of(NotificationPort.Channel.PUSH), null, null));
        assertThat(digestEntries.findPendingByUserId(userId)).hasSize(1);

        // The user opts out of digests; the held item must still reach them.
        NotificationSetting optedOut = settings.findByUserId(userId).orElseThrow();
        optedOut.update(null, null, "UTC", "NONE");
        settings.saveSetting(optedOut);

        assertThat(digestService.flush(userId, afterTomorrowsDigest())).isTrue();

        assertThat(digestEntries.findPendingByUserId(userId)).isEmpty();
        assertThat(deliveries.findByNotificationId(id))
                .extracting(NotificationDelivery::getChannel)
                .contains(NotificationChannel.IN_APP, NotificationChannel.PUSH);
    }

    @Test
    @DisplayName("Digest preferences are read back from the settings row")
    void digestPreferenceRoundTrip() {
        UUID userId = UUID.randomUUID();
        NotificationSetting setting = NotificationSetting.defaults(userId);
        setting.update(null, null, "Africa/Casablanca", "WEEKLY");
        settings.saveSetting(setting);

        assertThat(settings.findByUserId(userId).orElseThrow().getDigest()).isEqualTo("WEEKLY");
        assertThat(DigestFrequency.parse("weekly")).isEqualTo(DigestFrequency.WEEKLY);
    }
}
