package ma.noba.payment.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StripePaymentAdapterTest {

    @Test
    @DisplayName("Major-unit amounts convert to minor units without truncation")
    void convertsToMinorUnits() {
        assertThat(StripePaymentAdapter.toMinorUnits(new BigDecimal("100.00"))).isEqualTo(10000L);
        assertThat(StripePaymentAdapter.toMinorUnits(new BigDecimal("95.5"))).isEqualTo(9550L);
        assertThat(StripePaymentAdapter.toMinorUnits(new BigDecimal("0.01"))).isEqualTo(1L);
    }

    @Test
    @DisplayName("Extra decimals are rounded half-up rather than truncated")
    void roundsHalfUp() {
        assertThat(StripePaymentAdapter.toMinorUnits(new BigDecimal("1.005"))).isEqualTo(101L);
        assertThat(StripePaymentAdapter.toMinorUnits(new BigDecimal("1.004"))).isEqualTo(100L);
    }
}
