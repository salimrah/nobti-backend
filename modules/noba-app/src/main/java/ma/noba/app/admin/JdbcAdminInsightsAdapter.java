package ma.noba.app.admin;

import ma.noba.shared.domain.port.AdminInsightsPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Cross-module aggregates for the insights screen (users, providers, bookings, payments). */
@Component
public class JdbcAdminInsightsAdapter implements AdminInsightsPort {

    private static final String DAY = "to_char(%s at time zone 'UTC', 'YYYY-MM-DD')";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAdminInsightsAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Insights insights(Instant from, Instant to) {
        // JDBC cannot bind Instant directly; Timestamp keeps the timezone-aware comparison.
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("from", java.sql.Timestamp.from(from))
                .addValue("to", java.sql.Timestamp.from(to));

        long newUsers = count("select count(*) from users where created_at >= :from and created_at < :to", p);
        long newProviders = count("select count(*) from provider where created_at >= :from and created_at < :to", p);
        long bookings = count("select count(*) from appointment where created_at >= :from and created_at < :to", p);
        BigDecimal revenue = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from payment_transaction " +
                        "where status in ('SUCCEEDED', 'PARTIALLY_REFUNDED') " +
                        "and created_at >= :from and created_at < :to", p, BigDecimal.class);

        return new Insights(
                from, to, newUsers, newProviders, bookings, money(revenue),
                series("select " + String.format(DAY, "created_at") + " d, count(*) c from users " +
                        "where created_at >= :from and created_at < :to group by 1 order by 1", p),
                series("select " + String.format(DAY, "created_at") + " d, count(*) c from appointment " +
                        "where created_at >= :from and created_at < :to group by 1 order by 1", p),
                moneySeries("select " + String.format(DAY, "created_at") + " d, coalesce(sum(amount), 0) a " +
                        "from payment_transaction where status in ('SUCCEEDED', 'PARTIALLY_REFUNDED') " +
                        "and created_at >= :from and created_at < :to group by 1 order by 1", p));
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0 : value;
    }

    private List<Series> series(String sql, MapSqlParameterSource params) {
        return jdbc.query(sql, params, (rs, index) -> new Series(rs.getString("d"), rs.getLong("c")));
    }

    private List<MoneySeries> moneySeries(String sql, MapSqlParameterSource params) {
        return jdbc.query(sql, params, (rs, index) -> new MoneySeries(rs.getString("d"), money(rs.getBigDecimal("a"))));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
