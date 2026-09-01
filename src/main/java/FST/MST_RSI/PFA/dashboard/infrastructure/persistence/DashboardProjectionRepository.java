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
import FST.MST_RSI.PFA.dashboard.application.dto.VoipByRoleDto;
import FST.MST_RSI.PFA.dashboard.application.dto.VoipBySolutionDto;
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
        // Query from voice_call_session for rich metrics
        List<VoipSummaryDto> results = jdbcTemplate.query(
                """
                SELECT COUNT(*) AS total,
                       SUM(CASE WHEN answered_at IS NOT NULL OR outcome = 'ANSWERED' OR outcome = 'HANGUP' THEN 1 ELSE 0 END) AS answered,
                       SUM(CASE WHEN outcome = 'NO_ANSWER' THEN 1 ELSE 0 END) AS no_answer,
                       SUM(CASE WHEN outcome = 'REJECTED' THEN 1 ELSE 0 END) AS rejected,
                       SUM(CASE WHEN outcome = 'BUSY' THEN 1 ELSE 0 END) AS busy,
                       SUM(CASE WHEN outcome = 'FAILED' OR outcome = 'UNREACHABLE' OR outcome = 'UNREGISTERED' THEN 1 ELSE 0 END) AS failed,
                       AVG(CASE WHEN answered_at IS NOT NULL AND duration_seconds IS NOT NULL THEN duration_seconds END) AS avg_duration,
                       MIN(CASE WHEN answered_at IS NOT NULL AND duration_seconds > 0 THEN duration_seconds END) AS min_duration,
                       MAX(CASE WHEN answered_at IS NOT NULL THEN duration_seconds END) AS max_duration,
                       AVG(CASE WHEN answered_at IS NOT NULL AND started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (answered_at - started_at)) END) AS avg_ring_time,
                       SUM(CASE WHEN live_conversation = TRUE THEN 1 ELSE 0 END) AS manual_calls,
                       SUM(CASE WHEN live_conversation IS NOT TRUE THEN 1 ELSE 0 END) AS auto_calls
                FROM voice_call_session
                WHERE started_at >= ? AND started_at < ?
                """,
                (rs, rowNum) -> {
                    long total = rs.getLong("total");
                    long answered = rs.getLong("answered");
                    long noAnswer = rs.getLong("no_answer");
                    long rejected = rs.getLong("rejected");
                    long busy = rs.getLong("busy");
                    long failed = rs.getLong("failed");
                    double responseRate = total > 0 ? Math.round((double) answered / total * 1000.0) / 10.0 : 0.0;
                    double failureRate = total > 0 ? Math.round((double) failed / total * 1000.0) / 10.0 : 0.0;
                    Double avgDuration = rs.getObject("avg_duration") != null ? Math.round(rs.getDouble("avg_duration") * 10.0) / 10.0 : null;
                    Integer minDuration = rs.getObject("min_duration") != null ? rs.getInt("min_duration") : null;
                    Integer maxDuration = rs.getObject("max_duration") != null ? rs.getInt("max_duration") : null;
                    Double avgRingTime = rs.getObject("avg_ring_time") != null ? Math.round(rs.getDouble("avg_ring_time") * 10.0) / 10.0 : null;
                    long manual = rs.getLong("manual_calls");
                    long auto = rs.getLong("auto_calls");

                    return new VoipSummaryDto(
                            total,
                            answered,
                            noAnswer,
                            rejected,
                            busy,
                            failed,
                            responseRate,
                            failureRate,
                            avgDuration,
                            minDuration,
                            maxDuration,
                            avgRingTime,
                            manual,
                            auto
                    );
                },
                ts(from), ts(to)
        );

        if (!results.isEmpty() && results.getFirst().total() > 0) {
            return results.getFirst();
        }

        // Fallback to notification table if no session entries exist in time range
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

        long sent = counts.getOrDefault("SENT", 0L) + counts.getOrDefault("ACKNOWLEDGED", 0L);
        long failed = counts.getOrDefault("FAILED", 0L);
        long deferred = counts.getOrDefault("DEFERRED", 0L);
        long total = sent + failed + deferred + counts.getOrDefault("PENDING", 0L) + counts.getOrDefault("SKIPPED", 0L);

        return new VoipSummaryDto(total, sent, failed, deferred, counts.getOrDefault("PENDING", 0L));
    }

    public List<VoipCallDto> fetchRecentVoipCalls(Instant from, Instant to, int limit) {
        return jdbcTemplate.query(
                """
                SELECT vcs.id AS session_id,
                       vcs.notification_id,
                       vcs.alert_id,
                       vcs.outcome,
                       vcs.duration_seconds,
                       vcs.live_conversation AS live_mode,
                       vcs.hangup_source,
                       vcs.failure_reason,
                       vcs.started_at,
                       vcs.extension,
                       COALESCE(p.full_name, 'Destinataire') AS person_name,
                       re.current_step AS escalation_step
                FROM voice_call_session vcs
                LEFT JOIN person p ON p.id = vcs.person_id
                LEFT JOIN notification n ON n.id = vcs.notification_id
                LEFT JOIN routing_execution re ON re.id = COALESCE(vcs.routing_execution_id, n.routing_execution_id)
                WHERE vcs.started_at >= ? AND vcs.started_at < ?
                ORDER BY vcs.started_at DESC
                LIMIT ?
                """,
                (rs, rowNum) -> new VoipCallDto(
                        rs.getObject("notification_id") != null ? rs.getObject("notification_id", UUID.class).toString() : rs.getObject("session_id", UUID.class).toString(),
                        rs.getObject("alert_id") != null ? rs.getObject("alert_id", UUID.class).toString() : null,
                        rs.getString("person_name"),
                        rs.getString("extension") != null ? rs.getString("extension") : "SIP",
                        rs.getString("outcome"),
                        toInstant(rs.getTimestamp("started_at")),
                        rs.getObject("escalation_step") != null ? rs.getInt("escalation_step") : 1,
                        rs.getString("outcome"),
                        rs.getObject("duration_seconds") != null ? rs.getInt("duration_seconds") : null,
                        rs.getBoolean("live_mode"),
                        rs.getString("hangup_source"),
                        rs.getString("failure_reason")
                ),
                ts(from), ts(to), limit
        );
    }

    public List<VoipByRoleDto> fetchVoipByRole(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(uaa.role, 'NON_ASSIGNE') AS role,
                       COUNT(vcs.id) AS total_calls,
                       SUM(CASE WHEN vcs.answered_at IS NOT NULL OR vcs.outcome = 'ANSWERED' OR vcs.outcome = 'HANGUP' THEN 1 ELSE 0 END) AS answered_calls,
                       AVG(CASE WHEN vcs.answered_at IS NOT NULL AND vcs.duration_seconds IS NOT NULL THEN vcs.duration_seconds END) AS avg_duration
                FROM voice_call_session vcs
                LEFT JOIN unit_admin_assignment uaa ON uaa.person_id = vcs.person_id AND uaa.active = TRUE
                WHERE vcs.started_at >= ? AND vcs.started_at < ?
                GROUP BY COALESCE(uaa.role, 'NON_ASSIGNE')
                ORDER BY total_calls DESC
                """,
                (rs, rowNum) -> {
                    long total = rs.getLong("total_calls");
                    long answered = rs.getLong("answered_calls");
                    double responseRate = total > 0 ? Math.round((double) answered / total * 1000.0) / 10.0 : 0.0;
                    Double avgDuration = rs.getObject("avg_duration") != null ? Math.round(rs.getDouble("avg_duration") * 10.0) / 10.0 : null;
                    return new VoipByRoleDto(
                            rs.getString("role"),
                            total,
                            answered,
                            responseRate,
                            avgDuration
                    );
                },
                ts(from), ts(to)
        );
    }

    public List<VoipBySolutionDto> fetchVoipBySolution(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT ou.name AS solution_name,
                       COUNT(DISTINCT a.id) AS total_alerts,
                       COUNT(vcs.id) AS total_voip_calls,
                       SUM(CASE WHEN vcs.answered_at IS NOT NULL OR vcs.outcome = 'ANSWERED' OR vcs.outcome = 'HANGUP' THEN 1 ELSE 0 END) AS answered_calls
                FROM organizational_unit ou
                LEFT JOIN alert a ON a.resolved_solution_id = ou.id AND a.received_at >= ? AND a.received_at < ?
                LEFT JOIN voice_call_session vcs ON vcs.alert_id = a.id
                WHERE ou.unit_type = 'SOLUTION'
                GROUP BY ou.id, ou.name
                ORDER BY total_alerts DESC, ou.name
                """,
                (rs, rowNum) -> {
                    long totalCalls = rs.getLong("total_voip_calls");
                    long answered = rs.getLong("answered_calls");
                    double responseRate = totalCalls > 0 ? Math.round((double) answered / totalCalls * 1000.0) / 10.0 : 0.0;
                    return new VoipBySolutionDto(
                            rs.getString("solution_name"),
                            rs.getLong("total_alerts"),
                            totalCalls,
                            answered,
                            responseRate
                    );
                },
                ts(from), ts(to)
        );
    }

    public List<LabelCountDto> fetchVoipByEscalationStep(Instant from, Instant to) {
        return jdbcTemplate.query(
                """
                SELECT COALESCE(re.current_step::text, '1') AS label, COUNT(*) AS cnt
                FROM voice_call_session vcs
                LEFT JOIN routing_execution re ON re.id = vcs.routing_execution_id
                WHERE vcs.started_at >= ? AND vcs.started_at < ?
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
                       pcs.last_voip_outcome,
                       pcs.sip_reachability,
                       COUNT(n.id) AS total_notifs,
                       SUM(CASE WHEN n.notification_status = 'SENT' THEN 1 ELSE 0 END) AS success_count,
                       SUM(CASE WHEN n.notification_status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count,
                       SUM(CASE WHEN n.notification_type = 'VOIP' THEN 1 ELSE 0 END) AS voip_calls,
                       MAX(n.created_at) AS last_contact
                FROM person p
                LEFT JOIN person_contact_state pcs ON pcs.person_id = p.id
                LEFT JOIN notification_recipient nr ON nr.person_id = p.id
                LEFT JOIN notification n ON n.id = nr.notification_id
                    AND n.created_at >= ? AND n.created_at < ?
                WHERE p.active = TRUE
                   OR n.id IS NOT NULL
                GROUP BY p.id, p.full_name, p.email, p.phone, p.active, pcs.last_voip_outcome, pcs.sip_reachability
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
                            toInstant(rs.getTimestamp("last_contact")),
                            rs.getString("last_voip_outcome"),
                            rs.getString("sip_reachability") != null ? rs.getString("sip_reachability") : "UNKNOWN"
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
