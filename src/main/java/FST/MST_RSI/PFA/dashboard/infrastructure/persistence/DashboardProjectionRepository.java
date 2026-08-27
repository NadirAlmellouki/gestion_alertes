package FST.MST_RSI.PFA.dashboard.infrastructure.persistence;

import FST.MST_RSI.PFA.dashboard.application.dto.AdminAvailabilityDto;
import FST.MST_RSI.PFA.dashboard.application.dto.AlertKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.ChannelBreakdownDto;
import FST.MST_RSI.PFA.dashboard.application.dto.ClassificationKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.LabelCountDto;
import FST.MST_RSI.PFA.dashboard.application.dto.NotificationKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.ResolutionKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.RoutingKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.TimeSeriesPointDto;
import FST.MST_RSI.PFA.dashboard.application.dto.VoipCallDto;
import FST.MST_RSI.PFA.dashboard.application.dto.VoipSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class DashboardProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AlertKpiDto fetchAlertKpis(Instant from, Instant to) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert
                WHERE received_at >= ? AND received_at < ?
                """,
                Long.class, ts(from), ts(to)
        );
        Long open = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert
                WHERE received_at >= ? AND received_at < ?
                  AND (status = 'OPEN' OR end_time IS NULL)
                """,
                Long.class, ts(from), ts(to)
        );
        Long closed = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert
                WHERE received_at >= ? AND received_at < ?
                  AND (status = 'CLOSED' OR end_time IS NOT NULL)
                """,
                Long.class, ts(from), ts(to)
        );
        Long critical = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert
                WHERE received_at >= ? AND received_at < ?
                  AND severity IN ('ERROR', 'CRITICAL', 'AVAILABILITY', 'RESOURCE_CONTENTION', 'PERFORMANCE', 'MONITORING_UNAVAILABLE')
                """,
                Long.class, ts(from), ts(to)
        );
        Long humanValidation = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT ala.alert_id)
                FROM alert_llm_analysis ala
                JOIN alert a ON a.id = ala.alert_id
                WHERE a.received_at >= ? AND a.received_at < ?
                  AND ala.requires_human_validation = TRUE
                """,
                Long.class, ts(from), ts(to)
        );

        long totalCount = total != null ? total : 0L;
        long closedCount = closed != null ? closed : 0L;
        double resolutionRate = totalCount == 0 ? 0.0 : (closedCount * 100.0) / totalCount;

        return new AlertKpiDto(
                totalCount,
                open != null ? open : 0L,
                closedCount,
                critical != null ? critical : 0L,
                humanValidation != null ? humanValidation : 0L,
                Math.round(resolutionRate * 10.0) / 10.0
        );
    }

    public NotificationKpiDto fetchNotificationKpis(Instant from, Instant to) {
        List<ChannelBreakdownDto> byChannel = jdbcTemplate.query(
                """
                SELECT notification_type,
                       SUM(CASE WHEN notification_status = 'SENT' THEN 1 ELSE 0 END) AS sent,
                       SUM(CASE WHEN notification_status = 'FAILED' THEN 1 ELSE 0 END) AS failed,
                       SUM(CASE WHEN notification_status = 'DEFERRED' THEN 1 ELSE 0 END) AS deferred,
                       SUM(CASE WHEN notification_status = 'PENDING' THEN 1 ELSE 0 END) AS pending,
                       SUM(CASE WHEN notification_status = 'SKIPPED' THEN 1 ELSE 0 END) AS skipped
                FROM notification
                WHERE created_at >= ? AND created_at < ?
                GROUP BY notification_type
                ORDER BY notification_type
                """,
                (rs, rowNum) -> new ChannelBreakdownDto(
                        rs.getString("notification_type"),
                        rs.getLong("sent"),
                        rs.getLong("failed"),
                        rs.getLong("deferred"),
                        rs.getLong("pending"),
                        rs.getLong("skipped")
                ),
                ts(from), ts(to)
        );

        long total = 0, sent = 0, failed = 0, deferred = 0, pending = 0;
        for (ChannelBreakdownDto channel : byChannel) {
            total += channel.sent() + channel.failed() + channel.deferred() + channel.pending() + channel.skipped();
            sent += channel.sent();
            failed += channel.failed();
            deferred += channel.deferred();
            pending += channel.pending();
        }

        return new NotificationKpiDto(total, sent, failed, deferred, pending, byChannel);
    }

    public RoutingKpiDto fetchRoutingKpis(Instant from, Instant to) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM routing_execution
                WHERE started_at >= ? AND started_at < ?
                """,
                Long.class, ts(from), ts(to)
        );
        Long completed = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM routing_execution
                WHERE started_at >= ? AND started_at < ?
                  AND routing_status = 'COMPLETED'
                """,
                Long.class, ts(from), ts(to)
        );
        Long awaiting = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM routing_execution
                WHERE started_at >= ? AND started_at < ?
                  AND routing_status = 'AWAITING_ESCALATION'
                """,
                Long.class, ts(from), ts(to)
        );
        Long noPerson = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM routing_execution
                WHERE started_at >= ? AND started_at < ?
                  AND routing_status = 'NO_PERSON'
                """,
                Long.class, ts(from), ts(to)
        );
        Long escalationSteps = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM routing_history rh
                JOIN routing_execution re ON re.id = rh.routing_execution_id
                WHERE re.started_at >= ? AND re.started_at < ?
                """,
                Long.class, ts(from), ts(to)
        );

        return new RoutingKpiDto(
                total != null ? total : 0L,
                completed != null ? completed : 0L,
                awaiting != null ? awaiting : 0L,
                noPerson != null ? noPerson : 0L,
                escalationSteps != null ? escalationSteps : 0L
        );
    }

    public ResolutionKpiDto fetchResolutionKpis(Instant from, Instant to) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM resolution_check
                WHERE started_at >= ? AND started_at < ?
                """,
                Long.class, ts(from), ts(to)
        );
        Long resolved = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM resolution_check
                WHERE started_at >= ? AND started_at < ?
                  AND status = 'RESOLVED'
                """,
                Long.class, ts(from), ts(to)
        );
        Long active = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM resolution_check
                WHERE started_at >= ? AND started_at < ?
                  AND status = 'ACTIVE'
                """,
                Long.class, ts(from), ts(to)
        );
        Long expired = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM resolution_check
                WHERE started_at >= ? AND started_at < ?
                  AND status = 'EXPIRED'
                """,
                Long.class, ts(from), ts(to)
        );
        Long error = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM resolution_check
                WHERE started_at >= ? AND started_at < ?
                  AND status = 'ERROR'
                """,
                Long.class, ts(from), ts(to)
        );

        return new ResolutionKpiDto(
                total != null ? total : 0L,
                resolved != null ? resolved : 0L,
                active != null ? active : 0L,
                expired != null ? expired : 0L,
                error != null ? error : 0L
        );
    }

    public ClassificationKpiDto fetchClassificationKpis(Instant from, Instant to) {
        Long classified = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert_llm_analysis ala
                JOIN alert a ON a.id = ala.alert_id
                WHERE a.received_at >= ? AND a.received_at < ?
                  AND ala.status = 'SUCCESS'
                """,
                Long.class, ts(from), ts(to)
        );
        Long fallback = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM alert_llm_analysis ala
                JOIN alert a ON a.id = ala.alert_id
                WHERE a.received_at >= ? AND a.received_at < ?
                  AND ala.status = 'FALLBACK'
                """,
                Long.class, ts(from), ts(to)
        );
        Long humanValidation = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT ala.alert_id)
                FROM alert_llm_analysis ala
                JOIN alert a ON a.id = ala.alert_id
                WHERE a.received_at >= ? AND a.received_at < ?
                  AND ala.requires_human_validation = TRUE
                """,
                Long.class, ts(from), ts(to)
        );
        Double avgDuration = jdbcTemplate.queryForObject(
                """
                SELECT AVG(ala.duration_ms)
                FROM alert_llm_analysis ala
                JOIN alert a ON a.id = ala.alert_id
                WHERE a.received_at >= ? AND a.received_at < ?
                  AND ala.duration_ms IS NOT NULL
                """,
                Double.class, ts(from), ts(to)
        );

        return new ClassificationKpiDto(
                classified != null ? classified : 0L,
                fallback != null ? fallback : 0L,
                humanValidation != null ? humanValidation : 0L,
                avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : null
        );
    }

    public List<LabelCountDto> fetchAlertsBySeverity(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(severity, 'UNKNOWN') AS label, COUNT(*) AS cnt
                FROM alert
                WHERE received_at >= ? AND received_at < ?
                GROUP BY COALESCE(severity, 'UNKNOWN')
                ORDER BY cnt DESC
                """,
                this::mapLabelCount,
                ts(from), ts(to)
        );
    }

    public List<LabelCountDto> fetchAlertsByCategory(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(latest.category, 'UNKNOWN') AS label, COUNT(*) AS cnt
                FROM alert a
                LEFT JOIN LATERAL (
                    SELECT category
                    FROM alert_llm_analysis
                    WHERE alert_id = a.id
                    ORDER BY created_at DESC
                    LIMIT 1
                ) latest ON TRUE
                WHERE a.received_at >= ? AND a.received_at < ?
                GROUP BY COALESCE(latest.category, 'UNKNOWN')
                ORDER BY cnt DESC
                """,
                this::mapLabelCount,
                ts(from), ts(to)
        );
    }

    public List<TimeSeriesPointDto> fetchAlertTrend(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT date_trunc('hour', received_at) AS bucket, COUNT(*) AS cnt
                FROM alert
                WHERE received_at >= ? AND received_at < ?
                GROUP BY date_trunc('hour', received_at)
                ORDER BY bucket
                """,
                (rs, rowNum) -> new TimeSeriesPointDto(
                        toInstant(rs.getTimestamp("bucket")),
                        rs.getLong("cnt")
                ),
                ts(from), ts(to)
        );
    }

    public VoipSummaryDto fetchVoipSummary(Instant from, Instant to) {
        Map<String, Long> counts = new HashMap<>();
        jdbcTemplate.query(
                """
                SELECT notification_status, COUNT(*) AS cnt
                FROM notification
                WHERE notification_type = 'VOIP'
                  AND created_at >= ? AND created_at < ?
                GROUP BY notification_status
                """,
                rs -> {
                    counts.put(rs.getString("notification_status"), rs.getLong("cnt"));
                },
                ts(from), ts(to)
        );

        long sent = counts.getOrDefault("SENT", 0L);
        long failed = counts.getOrDefault("FAILED", 0L);
        long deferred = counts.getOrDefault("DEFERRED", 0L);
        long pending = counts.getOrDefault("PENDING", 0L);
        long total = sent + failed + deferred + pending + counts.getOrDefault("SKIPPED", 0L);

        return new VoipSummaryDto(total, sent, failed, deferred, pending);
    }

    public List<VoipCallDto> fetchRecentVoipCalls(Instant from, Instant to, int limit) {
        return jdbcTemplate.query(
                """
                SELECT n.id AS notification_id,
                       n.alert_id,
                       n.notification_status,
                       n.created_at,
                       nr.destination,
                       p.full_name AS person_name,
                       re.current_step AS escalation_step
                FROM notification n
                LEFT JOIN notification_recipient nr ON nr.notification_id = n.id
                LEFT JOIN person p ON p.id = nr.person_id
                LEFT JOIN routing_execution re ON re.id = n.routing_execution_id
                WHERE n.notification_type = 'VOIP'
                  AND n.created_at >= ? AND n.created_at < ?
                ORDER BY n.created_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new VoipCallDto(
                        rs.getObject("notification_id", UUID.class).toString(),
                        rs.getObject("alert_id", UUID.class).toString(),
                        rs.getString("person_name"),
                        rs.getString("destination"),
                        rs.getString("notification_status"),
                        toInstant(rs.getTimestamp("created_at")),
                        rs.getObject("escalation_step") != null ? rs.getInt("escalation_step") : null
                ),
                ts(from), ts(to), limit
        );
    }

    public List<LabelCountDto> fetchVoipByEscalationStep(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(re.current_step::text, 'unknown') AS label, COUNT(*) AS cnt
                FROM notification n
                LEFT JOIN routing_execution re ON re.id = n.routing_execution_id
                WHERE n.notification_type = 'VOIP'
                  AND n.created_at >= ? AND n.created_at < ?
                GROUP BY re.current_step
                ORDER BY re.current_step NULLS LAST
                """,
                this::mapLabelCount,
                ts(from), ts(to)
        );
    }

    public List<AdminAvailabilityDto> fetchAdminAvailability(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT p.id,
                       p.full_name,
                       p.email,
                       p.phone,
                       p.active,
                       COUNT(n.id) AS total_notifs,
                       SUM(CASE WHEN n.notification_status = 'SENT' THEN 1 ELSE 0 END) AS success_count,
                       SUM(CASE WHEN n.notification_status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                       SUM(CASE WHEN n.notification_type = 'VOIP' THEN 1 ELSE 0 END) AS voip_calls,
                       MAX(n.created_at) AS last_contact
                FROM person p
                LEFT JOIN notification_recipient nr ON nr.person_id = p.id
                LEFT JOIN notification n ON n.id = nr.notification_id
                    AND n.created_at >= ? AND n.created_at < ?
                WHERE p.active = TRUE
                   OR n.id IS NOT NULL
                GROUP BY p.id, p.full_name, p.email, p.phone, p.active
                ORDER BY COUNT(n.id) DESC, p.full_name
                LIMIT 100
                """,
                (rs, rowNum) -> {
                    boolean active = rs.getBoolean("active");
                    String phone = rs.getString("phone");
                    String email = rs.getString("email");
                    long success = rs.getLong("success_count");
                    long failed = rs.getLong("failed_count");
                    return new AdminAvailabilityDto(
                            rs.getObject("id", UUID.class).toString(),
                            rs.getString("full_name"),
                            email,
                            phone,
                            active,
                            deriveAvailability(active, phone, email, success, failed),
                            rs.getLong("total_notifs"),
                            success,
                            failed,
                            rs.getLong("voip_calls"),
                            toInstant(rs.getTimestamp("last_contact"))
                    );
                },
                ts(from), ts(to)
        );
    }

    static String deriveAvailability(boolean active, String phone, String email, long success, long failed) {
        if (!active) {
            return "INACTIVE";
        }
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (!hasPhone && hasEmail) {
            return "PARTIAL";
        }
        if (hasPhone && failed > 0 && failed >= success) {
            return "UNREACHABLE";
        }
        if (hasPhone && (success > 0 || (success == 0 && failed == 0))) {
            return "AVAILABLE";
        }
        if (hasEmail) {
            return "PARTIAL";
        }
        return "UNREACHABLE";
    }

    private LabelCountDto mapLabelCount(ResultSet rs, int rowNum) throws SQLException {
        return new LabelCountDto(rs.getString("label"), rs.getLong("cnt"));
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
