package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.application.dto.AlertDto;
import FST.MST_RSI.PFA.alerting.application.mapper.AlertMapper;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAlertDetailUseCase {

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertMapper alertMapper;

    public GetAlertDetailUseCase(AlertRepositoryPort alertRepositoryPort, AlertMapper alertMapper) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.alertMapper = alertMapper;
    }

    @Transactional(readOnly = true)
    public AlertDto execute(String alertId) {
        AlertId id = AlertId.of(alertId);
        return alertRepositoryPort.findById(id)
                .map(alertMapper::toDetail)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));
    }
}
