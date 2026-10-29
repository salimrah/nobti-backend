package ma.noba.shared.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Aggregated platform metrics for the admin insights screen. */
public interface AdminInsightsPort {

    Insights insights(Instant from, Instant to);

    record Insights(
            Instant from,
            Instant to,
            long newUsers,
            long newProviders,
            long bookings,
            BigDecimal revenue,
            List<Series> signupsByDay,
            List<Series> bookingsByDay,
            List<MoneySeries> revenueByDay) {
    }

    record Series(String date, long count) {
    }

    record MoneySeries(String date, BigDecimal amount) {
    }
}
