package FST.MST_RSI.PFA.classification.infrastructure.context;

import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import FST.MST_RSI.PFA.classification.domain.port.BusinessContextPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Fallback/test adapter loading the exported Excel catalog from JSON. */
@Component
@ConditionalOnProperty(prefix = "app.business-context", name = "source", havingValue = "file")
public class FileBusinessContextAdapter implements BusinessContextPort {

    private static final Logger log = LoggerFactory.getLogger(FileBusinessContextAdapter.class);
    private static final int MAX_CANDIDATES = 8;

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String catalogPath;
    private final List<SolutionContext> catalog = new ArrayList<>();

    public FileBusinessContextAdapter(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            @Value("${app.business-context.catalog-path:classpath:business-context/solutions-catalog.json}")
            String catalogPath
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.catalogPath = catalogPath;
    }

    @PostConstruct
    void loadCatalog() {
        try {
            Resource resource = resourceLoader.getResource(catalogPath);
            try (InputStream in = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                for (JsonNode node : root.path("solutions")) {
                    catalog.add(new SolutionContext(
                            text(node, "name"),
                            text(node, "entity"),
                            text(node, "pole"),
                            text(node, "domaine"),
                            text(node, "type"),
                            text(node, "psi"),
                            text(node, "typeService"),
                            text(node, "tenant"),
                            text(node, "description")
                    ));
                }
            }
            log.info("Loaded {} solutions into file business context from {}", catalog.size(), catalogPath);
        } catch (Exception ex) {
            log.warn("Business context catalog unavailable ({}): {}", catalogPath, ex.getMessage());
        }
    }

    @Override
    public List<SolutionContext> findCandidates(String... searchTerms) {
        if (searchTerms == null || searchTerms.length == 0) {
            return List.of();
        }

        List<Scored> scored = new ArrayList<>();
        for (SolutionContext solution : catalog) {
            int score = score(solution, searchTerms);
            if (score > 0) {
                scored.add(new Scored(solution, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return scored.stream()
                .limit(MAX_CANDIDATES)
                .map(Scored::solution)
                .toList();
    }

    @Override
    public Optional<String> findPsiBySolutionName(String solutionName) {
        if (solutionName == null || solutionName.isBlank()) {
            return Optional.empty();
        }
        return catalog.stream()
                .filter(item -> item.name() != null && item.name().equalsIgnoreCase(solutionName.trim()))
                .map(SolutionContext::psi)
                .filter(psi -> psi != null && !psi.isBlank())
                .findFirst();
    }

    private int score(SolutionContext solution, String[] terms) {
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

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record Scored(SolutionContext solution, int score) {
    }
}
