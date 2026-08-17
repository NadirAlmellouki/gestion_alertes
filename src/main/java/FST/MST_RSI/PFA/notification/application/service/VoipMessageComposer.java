package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import org.springframework.stereotype.Service;

@Service
public class VoipMessageComposer {

    public String compose(Alert alert, ClassificationResult classification, String recipientName) {
        String name = recipientName != null && !recipientName.isBlank() ? recipientName : "collègue";
        String solution = classification.matchedSolution() != null ? classification.matchedSolution() : "solution inconnue";
        return """
                Bonjour %s. AlertOps vous contacte pour une alerte Dynatrace.
                Problème %s sur %s.
                Titre : %s.
                Sévérité : %s.
                Merci de prendre connaissance de cette alerte dans Dynatrace.
                """.formatted(
                name,
                alert.getExternalProblemId(),
                solution,
                alert.getTitle(),
                alert.getSeverity() != null ? alert.getSeverity() : "non précisée"
        ).replaceAll("\\s+", " ").trim();
    }
}
