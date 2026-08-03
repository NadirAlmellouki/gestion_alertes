package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.application.dto.AlertSummaryDto;
import FST.MST_RSI.PFA.alerting.application.mapper.AlertMapper;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ListRecentAlertsUseCase {

    private static final long RECENT_WINDOW_HOURS = 72;

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertMapper alertMapper;

    public ListRecentAlertsUseCase(AlertRepositoryPort alertRepositoryPort, AlertMapper alertMapper) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.alertMapper = alertMapper;
    }

    @Transactional(readOnly = true)
    public List<AlertSummaryDto> execute() {
        Instant since = Instant.now().minus(RECENT_WINDOW_HOURS, ChronoUnit.HOURS);
        return alertMapper.toSummaryList(alertRepositoryPort.findRecentSince(since));
    }
}
