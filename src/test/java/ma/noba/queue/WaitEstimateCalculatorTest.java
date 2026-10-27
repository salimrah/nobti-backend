package ma.noba.queue;

import ma.noba.queue.application.dto.WaitEstimate;
import ma.noba.queue.application.service.WaitEstimateCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WaitEstimateCalculatorTest {

    @Test
    @DisplayName("No history yields a LOW-confidence, wide, non-negative range")
    void noHistory() {
        WaitEstimate estimate = WaitEstimateCalculator.forMember(30, 0, 0);

        assertThat(estimate.estimatedMinutes()).isEqualTo(30);
        assertThat(estimate.confidence()).isEqualTo("LOW");
        assertThat(estimate.minMinutes()).isGreaterThanOrEqualTo(0);
        assertThat(estimate.maxMinutes()).isGreaterThan(30);
        assertThat(estimate.basis()).contains("No recent history");
    }

    @Test
    @DisplayName("Rich history yields HIGH confidence and a tighter range")
    void richHistory() {
        WaitEstimate low = WaitEstimateCalculator.forMember(40, 1, 0);
        WaitEstimate high = WaitEstimateCalculator.forMember(40, 20, 0);

        assertThat(high.confidence()).isEqualTo("HIGH");
        assertThat(low.confidence()).isEqualTo("LOW");
        assertThat(high.maxMinutes() - high.minMinutes())
                .isLessThan(low.maxMinutes() - low.minMinutes());
    }

    @Test
    @DisplayName("Medium history yields MEDIUM confidence")
    void mediumHistory() {
        assertThat(WaitEstimateCalculator.forMember(20, 5, 0).confidence()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("People ahead widen the upper bound")
    void peopleAheadWidenRange() {
        WaitEstimate none = WaitEstimateCalculator.forMember(30, 10, 0);
        WaitEstimate many = WaitEstimateCalculator.forMember(30, 10, 5);

        assertThat(many.maxMinutes()).isGreaterThan(none.maxMinutes());
    }

    @Test
    @DisplayName("Estimate never produces a negative lower bound")
    void neverNegative() {
        assertThat(WaitEstimateCalculator.forMember(2, 0, 0).minMinutes()).isZero();
    }
}
