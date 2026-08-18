package FST.MST_RSI.PFA.audit.application.listener;

import FST.MST_RSI.PFA.alerting.domain.event.AlertReceivedEvent;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.classification.domain.event.AlertClassifiedEvent;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
public class AuditDomainEventListener {

    private final AuditRecorder auditRecorder;
    private final AlertLlmAnalysisRepository llmAnalysisRepository;

    public AuditDomainEventListener(AuditRecorder auditRecorder, AlertLlmAnalysisRepository llmAnalysisRepository) {
        this.auditRecorder = auditRecorder;
        this.llmAnalysisRepository = llmAnalysisRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertReceived(AlertReceivedEvent event) {
        UUID alertId = event.getAlertId().value();
        auditRecorder.record(new AuditRecord(
                AuditAction.ALERT_RECEIVED,
                alertId,
                null,
                null,
                null,
                null,
                "Alert",
                alertId,
                "Alerte Dynatrace reçue: " + event.getTitle()
                        + " (problemId=" + event.getExternalProblemId()
                        + ", application=" + event.getApplicationName() + ")",
                AuditRecorder.correlationId(alertId),
                null,
                List.of()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertClassified(AlertClassifiedEvent event) {
        UUID alertId = event.getAlertId().value();
        UUID analysisId = llmAnalysisRepository.findTopByAlertIdOrderByCreatedAtDesc(alertId)
                .map(AlertLlmAnalysisEntity::getId)
                .orElse(null);

        auditRecorder.record(new AuditRecord(
                AuditAction.CLASSIFICATION_COMPLETED,
                alertId,
                analysisId,
                null,
                null,
                null,
                "AlertLlmAnalysis",
                analysisId,
                "Classification LLM: category=" + event.getCategory()
                        + ", solution=" + event.getMatchedSolution()
                        + ", confidence=" + event.getConfidence().value()
                        + ", psi=" + event.getResolvedPsi()
                        + ", status=" + event.getStatus()
                        + ", durationMs=" + event.getDurationMillis(),
                AuditRecorder.correlationId(alertId),
                null,
                List.of(
                        new AuditRecord.AuditDetail("provider", null, event.getProvider()),
                        new AuditRecord.AuditDetail("promptVersion", null, event.getPromptVersion())
                )
        ));
    }
}
