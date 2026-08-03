package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.application.dto.AlertSummaryDto;
import FST.MST_RSI.PFA.alerting.application.mapper.AlertMapper;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ListAlertHistoryUseCase {

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertMapper alertMapper;

    public ListAlertHistoryUseCase(AlertRepositoryPort alertRepositoryPort, AlertMapper alertMapper) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.alertMapper = alertMapper;
    }

    @Transactional(readOnly = true)
    public List<AlertSummaryDto> execute(Instant from, Instant to, int page, int size) {
        if (from.isAfter(to)) {
            throw new BusinessException("INVALID_DATE_RANGE", "Parameter 'from' must be before 'to'");
        }
        if (page < 0 || size <= 0 || size > 200) {
            throw new BusinessException("INVALID_PAGINATION", "Invalid pagination parameters");
        }
        return alertMapper.toSummaryList(alertRepositoryPort.findHistory(from, to, page, size));
    }
}
