package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.model.AlertTimelineEntry;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class AlertRepositoryAdapter implements AlertRepositoryPort {

    private final SpringDataAlertRepository repository;

    public AlertRepositoryAdapter(SpringDataAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = repository.findById(alert.getId().value())
                .orElseGet(AlertEntity::new);

        mapToEntity(alert, entity);
        entity.getTimeline().clear();
        for (AlertTimelineEntry entry : alert.getTimeline()) {
            entity.getTimeline().add(new AlertTimelineEntryEntity(
                    entity,
                    entry.getEventType(),
                    entry.getMessage(),
                    entry.getOccurredAt()
            ));
        }

        AlertEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Alert> findById(AlertId id) {
        return repository.findWithTimelineById(id.value()).map(this::mapToDomain);
    }

    @Override
    public Optional<Alert> findByExternalProblemId(String externalProblemId) {
        return repository.findWithTimelineByExternalProblemId(externalProblemId).map(this::mapToDomain);
    }

    @Override
    public List<Alert> findRecentSince(Instant since) {
        return repository.findByReceivedAtAfterOrderByReceivedAtDesc(since).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<Alert> findHistory(Instant from, Instant to, int page, int size) {
        return repository.findByReceivedAtBetweenOrderByReceivedAtDesc(from, to, PageRequest.of(page, size))
                .map(this::mapToDomain)
                .getContent();
    }

    private void mapToEntity(Alert alert, AlertEntity entity) {
        entity.setId(alert.getId().value());
        entity.setExternalProblemId(alert.getExternalProblemId());
        entity.setTitle(alert.getTitle());
        entity.setApplicationName(alert.getApplicationName());
        entity.setEnvironment(alert.getEnvironment());
        entity.setSeverity(alert.getSeverity());
        entity.setImpact(alert.getImpact());
        entity.setDynatraceState(alert.getDynatraceState());
        entity.setNotificationState(alert.getNotificationState());
        entity.setProblemUrl(alert.getProblemUrl());
        entity.setHostName(alert.getHostName());
        entity.setRawPayload(alert.getRawPayload());
        entity.setReceivedAt(alert.getReceivedAt());
        entity.setProblemStartedAt(alert.getProblemStartedAt());
    }

    private Alert mapToDomain(AlertEntity entity) {
        List<AlertTimelineEntry> timeline = entity.getTimeline().stream()
                .map(entry -> new AlertTimelineEntry(
                        entry.getId(),
                        entry.getEventType(),
                        entry.getMessage(),
                        entry.getOccurredAt()
                ))
                .toList();

        return Alert.restore(
                new AlertId(entity.getId()),
                entity.getExternalProblemId(),
                entity.getTitle(),
                entity.getApplicationName(),
                entity.getEnvironment(),
                entity.getSeverity(),
                entity.getImpact(),
                entity.getDynatraceState(),
                entity.getNotificationState(),
                entity.getProblemUrl(),
                entity.getHostName(),
                entity.getRawPayload(),
                entity.getReceivedAt(),
                entity.getProblemStartedAt(),
                timeline
        );
    }
}
