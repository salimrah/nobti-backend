package ma.noba.notification;

import ma.noba.notification.application.service.NotificationAnalyticsService;
import ma.noba.notification.domain.model.DeliveryStatus;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.port.NotificationAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAnalyticsServiceTest {

    @Mock NotificationAnalyticsRepository analytics;

    private NotificationAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new NotificationAnalyticsService(analytics);
    }

    private void stubAggregates() {
        when(analytics.countTotal(any(), any())).thenReturn(12L);
        when(analytics.countUnread(any(), any())).thenReturn(3L);
        when(analytics.countByCategory(any(), any())).thenReturn(List.of(
                new NotificationAnalyticsRepository.CategoryTotal(NotificationCategory.TRANSACTIONAL, 9),
                new NotificationAnalyticsRepository.CategoryTotal(NotificationCategory.MARKETING, 3)));
        when(analytics.countByType(any(), any())).thenReturn(List.of(
                new NotificationAnalyticsRepository.TypeTotal("queue.turn_start", 5)));
        when(analytics.countByChannelAndStatus(any(), any())).thenReturn(List.of(
                new NotificationAnalyticsRepository.ChannelStatusTotal(
                        NotificationChannel.PUSH, DeliveryStatus.SENT, 8)));
    }

    @Test
    @DisplayName("Summary exposes totals, categories and per-channel statuses")
    void summary() {
        stubAggregates();
        Instant to = Instant.parse("2026-10-28T00:00:00Z");

        var response = service.summary(to.minus(Duration.ofDays(7)), to);

        assertThat(response.total()).isEqualTo(12);
        assertThat(response.unread()).isEqualTo(3);
        assertThat(response.byCategory()).hasSize(2);
        assertThat(response.byChannel()).containsExactly(
                new ma.noba.notification.application.dto.NotificationAnalyticsResponse.ChannelStatusCount(
                        "PUSH", "SENT", 8));
    }

    @Test
    @DisplayName("CSV export has one row per metric")
    void csv() {
        stubAggregates();
        Instant to = Instant.parse("2026-10-28T00:00:00Z");

        String csv = service.csv(to.minus(Duration.ofDays(7)), to);

        assertThat(csv).startsWith("metric,key,status,count\n");
        assertThat(csv).contains("summary,total,,12");
        assertThat(csv).contains("summary,unread,,3");
        assertThat(csv).contains("category,TRANSACTIONAL,,9");
        assertThat(csv).contains("type,queue.turn_start,,5");
        assertThat(csv).contains("channel,PUSH,SENT,8");
    }

    @Test
    @DisplayName("A reversed range is rejected")
    void invalidRange() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> service.summary(now, now.minusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Ranges longer than a year are rejected")
    void tooLongRange() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> service.summary(now.minus(Duration.ofDays(400)), now))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
