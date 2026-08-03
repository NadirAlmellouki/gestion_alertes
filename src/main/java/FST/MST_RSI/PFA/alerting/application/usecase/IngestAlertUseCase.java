package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.event.AlertReceivedEvent;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.RawAlertPayload;
import FST.MST_RSI.PFA.alerting.domain.model.ValidationResult;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.alerting.domain.service.AlertPayloadValidator;
import FST.MST_RSI.PFA.alerting.domain.service.DynatraceAlertNormalizer;
import FST.MST_RSI.PFA.alerting.domain.service.DynatraceAlertNormalizer.NormalizedAlertData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestAlertUseCase.class);

    private final AlertPayloadValidator alertPayloadValidator;
    private final DynatraceAlertNormalizer normalizer;
    private final AlertRepositoryPort alertRepositoryPort;
    private final ApplicationEventPublisher eventPublisher;

    public IngestAlertUseCase(
            AlertPayloadValidator alertPayloadValidator,
            DynatraceAlertNormalizer normalizer,
            AlertRepositoryPort alertRepositoryPort,
            ApplicationEventPublisher eventPublisher
    ) {
        this.alertPayloadValidator = alertPayloadValidator;
        this.normalizer = normalizer;
        this.alertRepositoryPort = alertRepositoryPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IngestAlertResult execute(String jsonBody) {
        ValidationResult validation = alertPayloadValidator.validate(new RawAlertPayload(jsonBody));
        if (!validation.valid()) {
            log.warn("Dynatrace payload rejected: {}", validation.errors());
            return IngestAlertResult.rejected(validation.errors());
        }

        NormalizedAlertData data = normalizer.normalize(jsonBody);
        var existing = alertRepositoryPort.findByExternalProblemId(data.externalProblemId());

        Alert alert;
        boolean created;
        if (existing.isPresent()) {
            alert = existing.get();
            alert.updateFromDynatrace(
                    data.title(),
                    data.applicationName(),
                    data.environment(),
                    data.severity(),
                    data.impact(),
                    data.dynatraceState(),
                    data.problemUrl(),
                    data.hostName(),
                    data.rawPayload(),
                    data.problemStartedAt()
            );
            created = false;
        } else {
            alert = Alert.createNew(
                    data.externalProblemId(),
                    data.title(),
                    data.applicationName(),
                    data.environment(),
                    data.severity(),
                    data.impact(),
                    data.dynatraceState(),
                    data.problemUrl(),
                    data.hostName(),
                    data.rawPayload(),
                    data.problemStartedAt()
            );
            created = true;
        }

        Alert saved = alertRepositoryPort.save(alert);
        eventPublisher.publishEvent(new AlertReceivedEvent(
                saved.getId(),
                saved.getExternalProblemId(),
                saved.getTitle(),
                saved.getApplicationName()
        ));

        log.info("Alert ingested: id={}, externalProblemId={}, created={}",
                saved.getId().value(), saved.getExternalProblemId(), created);

        return IngestAlertResult.accepted(saved, created);
    }

    public record IngestAlertResult(
            boolean accepted,
            Alert alert,
            boolean created,
            java.util.List<String> errors
    ) {
        public static IngestAlertResult accepted(Alert alert, boolean created) {
            return new IngestAlertResult(true, alert, created, java.util.List.of());
        }

        public static IngestAlertResult rejected(java.util.List<String> errors) {
            return new IngestAlertResult(false, null, false, errors);
        }
    }
}
