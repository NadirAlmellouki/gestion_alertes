package FST.MST_RSI.PFA.classification.infrastructure.context;

import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import FST.MST_RSI.PFA.classification.domain.port.BusinessContextPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.business-context", name = "source", havingValue = "postgres")
public class PostgresBusinessContextAdapter implements BusinessContextPort {

    private static final int MAX_CANDIDATES = 8;
    private static final int SQL_PREFETCH_LIMIT = 40;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<SolutionContext> findCandidates(String... searchTerms) {
        if (searchTerms == null || searchTerms.length == 0) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT s.name,
                       e.name AS entity_name,
                       p.name AS pole_name,
                       d.name AS domain_name,
                       sa.solution_type,
                       sa.psi,
                       sa.service_type,
                       sa.tenant,
                       sa.functional_description
                  FROM organizational_unit s
                  JOIN organizational_unit d ON d.id = s.parent_unit_id AND d.unit_type = 'DOMAIN'
                  JOIN organizational_unit p ON p.id = d.parent_unit_id AND p.unit_type = 'POLE'
                  JOIN organizational_unit e ON e.id = p.parent_unit_id AND e.unit_type = 'ENTITY'
                  JOIN solution_attribute sa ON sa.unit_id = s.id
                 WHERE s.unit_type = 'SOLUTION'
                   AND s.active = TRUE
                   AND sa.active = TRUE
                   AND (
                """);

        List<String> params = new ArrayList<>();
        for (int i = 0; i < searchTerms.length; i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("""
                    lower(s.name) LIKE lower(?) OR
                    lower(d.name) LIKE lower(?) OR
                    lower(p.name) LIKE lower(?) OR
                    lower(e.name) LIKE lower(?) OR
                    lower(coalesce(sa.functional_description, '')) LIKE lower(?)
                    """);
            String pattern = "%" + searchTerms[i].trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(") LIMIT ").append(SQL_PREFETCH_LIMIT);

        var query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        List<Object[]> rows = query.getResultList();
        List<Scored> scored = new ArrayList<>();
        for (Object[] row : rows) {
            SolutionContext context = new SolutionContext(
                    asString(row[0]),
                    asString(row[1]),
                    asString(row[2]),
                    asString(row[3]),
                    asString(row[4]),
                    asString(row[5]),
                    asString(row[6]),
                    asString(row[7]),
                    asString(row[8])
            );
            scored.add(new Scored(context, score(context, searchTerms)));
        }

        return scored.stream()
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(MAX_CANDIDATES)
                .map(Scored::context)
                .toList();
    }

    @Override
    public Optional<String> findPsiBySolutionName(String solutionName) {
        if (solutionName == null || solutionName.isBlank()) {
            return Optional.empty();
        }
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT sa.psi
                          FROM organizational_unit s
                          JOIN solution_attribute sa ON sa.unit_id = s.id
                         WHERE s.unit_type = 'SOLUTION'
                           AND lower(s.name) = lower(?)
                         LIMIT 1
                        """)
                .setParameter(1, solutionName.trim())
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(rows.get(0)));
    }

    private static int score(SolutionContext solution, String[] terms) {
        int score = 0;
        String haystack = (safe(solution.name()) + " " + safe(solution.entity()) + " "
                + safe(solution.domaine()) + " " + safe(solution.pole()) + " "
                + safe(solution.description())).toLowerCase(Locale.ROOT);

        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            String needle = term.toLowerCase(Locale.ROOT);
            if (safe(solution.name()).equalsIgnoreCase(term)) {
                score += 100;
            } else if (safe(solution.name()).toLowerCase(Locale.ROOT).contains(needle)) {
                score += 40;
            } else if (haystack.contains(needle)) {
                score += 10;
            }
        }
        return score;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Scored(SolutionContext context, int score) {
    }
}
