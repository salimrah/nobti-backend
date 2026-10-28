package ma.noba.notification;

import ma.noba.notification.application.service.NotificationEventExportService;
import ma.noba.notification.application.service.NotificationPseudonymizer;
import ma.noba.notification.domain.model.NotificationCategory;
import ma.noba.notification.domain.model.NotificationChannel;
import ma.noba.notification.domain.port.NotificationEventExportRepository;
import ma.noba.notification.domain.port.NotificationEventExportRepository.EventRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventExportServiceTest {

    @Mock NotificationEventExportRepository repository;
    @Mock NotificationPseudonymizer pseudonymizer;

    private NotificationEventExportService service;
    private final Instant from = Instant.parse("2026-10-01T00:00:00Z");
    private final Instant to = Instant.parse("2026-10-29T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new NotificationEventExportService(repository, pseudonymizer);
    }

    private EventRow row(long id, String eventType, NotificationChannel channel, UUID userId) {
        return new EventRow(id, Instant.parse("2026-10-28T10:00:00Z"), eventType,
                "queue.turn_start", NotificationCategory.TRANSACTIONAL, channel, userId);
    }

    @Test
    @DisplayName("Rows are exported with a stable header and pseudonymized users")
    void exportsCsv() {
        UUID userId = UUID.randomUUID();
        when(repository.findEvents(eq(from), eq(to), eq(0L), eq(1000)))
                .thenReturn(List.of(row(7, "CREATED", null, userId), row(8, "SENT", NotificationChannel.PUSH, userId)));
        when(pseudonymizer.apply(userId)).thenReturn("u_abc123");

        var page = service.events(from, to, 0, 1000);

        assertThat(page.csv()).startsWith("event_id,occurred_at,event_type,notification_type,category,channel,pseudo_user\n");
        assertThat(page.csv()).contains("7,2026-10-28T10:00:00Z,CREATED,queue.turn_start,TRANSACTIONAL,,u_abc123");
        assertThat(page.csv()).contains("8,2026-10-28T10:00:00Z,SENT,queue.turn_start,TRANSACTIONAL,PUSH,u_abc123");
        assertThat(page.csv()).doesNotContain(userId.toString());
        assertThat(page.nextAfter()).isEqualTo(8);
        assertThat(page.rows()).isEqualTo(2);
    }

    @Test
    @DisplayName("An empty page keeps the cursor where it was")
    void emptyPage() {
        when(repository.findEvents(any(), any(), anyLong(), anyInt())).thenReturn(List.of());

        var page = service.events(from, to, 42, 100);

        assertThat(page.rows()).isZero();
        assertThat(page.nextAfter()).isEqualTo(42);
        assertThat(page.csv()).isEqualTo(NotificationEventExportService.HEADER);
    }

    @Test
    @DisplayName("The page size is clamped to 5000")
    void clampsLimit() {
        when(repository.findEvents(any(), any(), anyLong(), anyInt())).thenReturn(List.of());

        service.events(from, to, 0, 99_999);

        verify(repository).findEvents(from, to, 0L, 5000);
    }

    @Test
    @DisplayName("A reversed or default range is handled")
    void rangeValidation() {
        assertThatThrownBy(() -> service.events(to, from, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
