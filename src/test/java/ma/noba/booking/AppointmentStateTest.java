package ma.noba.booking;

import ma.noba.booking.domain.model.Appointment;
import ma.noba.booking.domain.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for appointment state-transition guards — a finished appointment must not be
 * silently overwritten (e.g. CANCELLED → COMPLETED).
 */
class AppointmentStateTest {

    private Appointment appointment() {
        return new Appointment(UUID.randomUUID(), UUID.randomUUID(), null,
                Instant.now(), Instant.now().plusSeconds(3600), null);
    }

    @Test
    @DisplayName("A pending appointment can be confirmed, then completed")
    void happyPath() {
        Appointment a = appointment();
        a.confirm();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        a.complete();
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("A pending appointment cannot be completed directly")
    void cannotCompletePending() {
        Appointment a = appointment();
        assertThatThrownBy(a::complete).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A cancelled appointment cannot be confirmed or completed")
    void cannotReviveCancelled() {
        Appointment a = appointment();
        a.cancel();
        assertThatThrownBy(a::confirm).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(a::complete).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A completed appointment cannot be cancelled or marked no-show")
    void cannotReopenCompleted() {
        Appointment a = appointment();
        a.confirm();
        a.complete();
        assertThatThrownBy(a::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(a::markNoShow).isInstanceOf(IllegalStateException.class);
    }
}
