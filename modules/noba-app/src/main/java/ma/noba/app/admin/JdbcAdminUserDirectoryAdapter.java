package ma.noba.app.admin;

import ma.noba.identity.domain.service.HashService;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.crypto.AesGcmStringAttributeConverter;
import ma.noba.shared.domain.model.UserStatus;
import ma.noba.shared.domain.port.AdminUserDirectoryPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-module read model for the admin console. Lives in the composition root because it joins
 * identity ({@code users}) with user ({@code user_profile}) and roles ({@code user_role}).
 * Emails and phones are encrypted at rest, so they are decrypted here and an exact contact search
 * goes through the identity hashes.
 */
@Component
public class JdbcAdminUserDirectoryAdapter implements AdminUserDirectoryPort {

    private static final AesGcmStringAttributeConverter PII = new AesGcmStringAttributeConverter();

    private static final String FILTERS = """
            where (
                    (cast(:query as text) = '' or lower(coalesce(p.first_name, '') || ' ' || coalesce(p.last_name, '')
                        || ' ' || coalesce(p.display_name, '')) like :like)
                    or (cast(:emailHash as text) is not null and u.primary_email_hash = cast(:emailHash as text))
                    or (cast(:phoneHash as text) is not null and u.primary_phone_hash = cast(:phoneHash as text))
                  )
              and (cast(:status as text) is null or u.status = cast(:status as text))
              and (cast(:role as text) = '' or exists (
                    select 1 from user_role ur2
                    join role r2 on r2.id = ur2.role_id
                    where ur2.user_id = u.id and r2.name = cast(:role as text)))
            """;

    private static final String FROM = """
            from users u
            left join user_profile p on p.user_id = u.id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final HashService hashes;

    public JdbcAdminUserDirectoryAdapter(NamedParameterJdbcTemplate jdbc, HashService hashes) {
        this.jdbc = jdbc;
        this.hashes = hashes;
    }

    @Override
    public PagedResponse<UserRow> searchUsers(String query, UserStatus status, String role, int page, int size) {
        String term = query == null ? "" : query.trim();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", term)
                .addValue("like", "%" + term.toLowerCase(Locale.ROOT) + "%")
                .addValue("emailHash", term.contains("@") ? hashes.hash(term.toLowerCase(Locale.ROOT)) : null)
                .addValue("phoneHash", term.matches("^\\+?[0-9]{6,}$") ? hashes.hash(term) : null)
                .addValue("status", status == null ? null : status.name())
                .addValue("role", normalizedRole)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);

        String sql = """
                select u.id, p.first_name, p.last_name, u.primary_email, u.primary_phone,
                       u.status, u.created_at,
                       coalesce(string_agg(r.name, ',' order by r.name), '') as roles
                """ + FROM + """
                left join user_role ur on ur.user_id = u.id
                left join role r on r.id = ur.role_id
                """ + FILTERS + """
                group by u.id, p.first_name, p.last_name, u.primary_email, u.primary_phone,
                         u.status, u.created_at
                order by u.created_at desc
                limit :limit offset :offset
                """;

        List<UserRow> rows = jdbc.query(sql, params, (rs, index) -> new UserRow(
                rs.getObject("id", UUID.class),
                rs.getString("first_name"),
                rs.getString("last_name"),
                PII.convertToEntityAttribute(rs.getString("primary_email")),
                PII.convertToEntityAttribute(rs.getString("primary_phone")),
                UserStatus.valueOf(rs.getString("status")),
                parseRoles(rs.getString("roles")),
                rs.getTimestamp("created_at").toInstant()));

        Long total = jdbc.queryForObject("select count(distinct u.id) " + FROM + FILTERS, params, Long.class);
        return PagedResponse.of(rows, page, size, total == null ? 0 : total);
    }

    @Override
    public Map<UUID, String> displayNames(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                select u.id, p.first_name, p.last_name, p.display_name
                from users u
                left join user_profile p on p.user_id = u.id
                where u.id in (:ids)
                """,
                new MapSqlParameterSource("ids", userIds));
        Map<UUID, String> names = new HashMap<>();
        for (Map<String, Object> row : rows) {
            UUID id = (UUID) row.get("id");
            String first = (String) row.get("first_name");
            String last = (String) row.get("last_name");
            String display = (String) row.get("display_name");
            String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            names.put(id, name.isBlank() ? (display == null || display.isBlank() ? "Client" : display) : name);
        }
        return names;
    }

    private static Set<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(roles.split(",")));
    }
}
