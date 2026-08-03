package FST.MST_RSI.PFA.alerting.api.webhook;

import FST.MST_RSI.PFA.alerting.application.dto.IngestAlertErrorResponse;
import FST.MST_RSI.PFA.alerting.application.dto.IngestAlertResponse;
import FST.MST_RSI.PFA.alerting.application.usecase.IngestAlertUseCase;
import FST.MST_RSI.PFA.alerting.application.usecase.IngestAlertUseCase.IngestAlertResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion/dynatrace")
public class DynatraceWebhookController {

    private final IngestAlertUseCase ingestAlertUseCase;

    public DynatraceWebhookController(IngestAlertUseCase ingestAlertUseCase) {
        this.ingestAlertUseCase = ingestAlertUseCase;
    }

    @PostMapping
    public ResponseEntity<?> ingest(@RequestBody String payload) {
        IngestAlertResult result = ingestAlertUseCase.execute(payload);

        if (!result.accepted()) {
            return ResponseEntity.badRequest()
                    .body(new IngestAlertErrorResponse("INVALID_PAYLOAD", result.errors()));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IngestAlertResponse(
                        result.alert().getId().value().toString(),
                        result.alert().getExternalProblemId(),
                        result.created()
                ));
    }
}
