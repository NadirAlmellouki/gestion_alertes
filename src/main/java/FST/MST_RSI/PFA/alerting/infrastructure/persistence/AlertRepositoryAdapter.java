package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class AlertRepositoryAdapter implements AlertRepositoryPort {

    private final SpringDataAlertRepository repository;
    private final ObjectMapper objectMapper;
    private final DynatracePayloadReader payloadReader;

    public AlertRepositoryAdapter(
            SpringDataAlertRepository repository,
            ObjectMapper objectMapper,
            DynatracePayloadReader payloadReader
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.payloadReader = payloadReader;
    }

    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = repository.findById(alert.getId().value())
                .orElseGet(AlertEntity::new);

        mapToEntity(alert, entity);
        AlertEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Alert> findById(AlertId id) {
        return repository.findById(id.value()).map(this::mapToDomain);
    }

    @Override
    public Optional<Alert> findByExternalProblemId(String externalProblemId) {
        return repository.findByProblemId(externalProblemId).map(this::mapToDomain);
    }

    @Override
    public List<Alert> findRecentSince(Instant since) {
        return repository.findByReceivedAtAfterOrderByReceivedAtDesc(since).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<Alert> findHistory(Instant from, Instant to, int page, int size) {
        return repository.findByReceivedAtBetweenOrderByReceivedAtDesc(from, to, org.springframework.data.domain.PageRequest.of(page, size))
                .map(this::mapToDomain)
                .getContent();
    }

    private void mapToEntity(Alert alert, AlertEntity entity) {
        JsonNode root = payloadReader.readTree(alert.getRawPayload());

        entity.setId(alert.getId().value());
        entity.setProblemId(alert.getExternalProblemId());
        entity.setDisplayId(payloadReader.text(root, "displayId"));
        entity.setTitle(alert.getTitle());
        entity.setSeverity(alert.getSeverity());
        entity.setImpactLevel(alert.getImpact());
        entity.setStatus(alert.getDynatraceState());
        entity.setSource("DYNATRACE");
        entity.setLinkedProblemId(payloadReader.text(root.path("linkedProblemInfo"), "problemId"));
        entity.setStartTime(alert.getProblemStartedAt());
        entity.setReceivedAt(alert.getReceivedAt());
        entity.setRawPayload(alert.getRawPayload());
        entity.setEntityTags(writeJson(root.get("entityTags")));
        entity.setImpactAnalysis(writeJson(root.get("impactAnalysis")));
        entity.setEvidenceDetails(writeJson(root.get("evidenceDetails")));
        entity.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : alert.getReceivedAt());
        entity.setNotificationState(alert.getNotificationState());
    }

    private Alert mapToDomain(AlertEntity entity) {
        JsonNode root = payloadReader.readTree(entity.getRawPayload());

        return Alert.restore(
                new AlertId(entity.getId()),
                entity.getProblemId(),
                entity.getTitle(),
                payloadReader.extractApplicationName(root),
                payloadReader.extractEnvironment(root),
                entity.getSeverity(),
                entity.getImpactLevel(),
                entity.getStatus(),
                entity.getNotificationState(),
                payloadReader.extractProblemUrl(root),
                payloadReader.extractHostName(root),
                entity.getRawPayload(),
                entity.getReceivedAt(),
                entity.getStartTime()
        );
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return null;
        }
    }
}
