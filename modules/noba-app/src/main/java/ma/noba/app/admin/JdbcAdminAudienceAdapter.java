package ma.noba.app.admin;

import ma.noba.shared.domain.port.AdminAudiencePort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JdbcAdminAudienceAdapter implements AdminAudiencePort {

    private static final String ACTIVE = "u.status = 'ACTIVE' and u.deleted_at is null";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAdminAudienceAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long count(Audience audience) {
        Long total = jdbc.queryForObject("select count(*) from users u where " + predicate(audience),
                new MapSqlParameterSource(), Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public List<UUID> page(Audience audience, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        return jdbc.queryForList(
                "select u.id from users u where " + predicate(audience)
                        + " order by u.created_at asc limit :limit offset :offset",
                params, UUID.class);
    }

    private static String predicate(Audience audience) {
        return switch (audience) {
            case CLIENTS -> ACTIVE
                    + " and exists (select 1 from user_role ur join role r on r.id = ur.role_id"
                    + " where ur.user_id = u.id and r.name = 'USER')"
                    + " and not exists (select 1 from user_role ur join role r on r.id = ur.role_id"
                    + " where ur.user_id = u.id and r.name = 'PROVIDER')";
            case PROVIDERS -> ACTIVE
                    + " and exists (select 1 from user_role ur join role r on r.id = ur.role_id"
                    + " where ur.user_id = u.id and r.name = 'PROVIDER')";
            case ALL -> ACTIVE;
        };
    }
}
