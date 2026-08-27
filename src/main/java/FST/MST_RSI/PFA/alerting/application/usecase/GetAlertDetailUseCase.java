package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.application.dto.AlertDto;
import FST.MST_RSI.PFA.alerting.application.mapper.AlertMapper;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisRepository;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAlertDetailUseCase {

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertLlmAnalysisRepository alertLlmAnalysisRepository;
    private final AlertMapper alertMapper;

    public GetAlertDetailUseCase(
            AlertRepositoryPort alertRepositoryPort,
            AlertLlmAnalysisRepository alertLlmAnalysisRepository,
            AlertMapper alertMapper
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.alertLlmAnalysisRepository = alertLlmAnalysisRepository;
        this.alertMapper = alertMapper;
    }

    @Transactional(readOnly = true)
    public AlertDto execute(String alertId) {
        AlertId id = AlertId.of(alertId);
        Alert alert = alertRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
        return alertLlmAnalysisRepository.findTopByAlertIdOrderByCreatedAtDesc(alert.getId().value())
                .map(analysis -> alertMapper.toDetail(alert, analysis))
                .orElseGet(() -> alertMapper.toDetail(alert));
    }
}
