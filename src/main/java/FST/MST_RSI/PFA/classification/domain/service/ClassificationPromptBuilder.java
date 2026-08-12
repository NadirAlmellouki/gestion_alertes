package FST.MST_RSI.PFA.classification.domain.service;

import FST.MST_RSI.PFA.classification.domain.model.AlertClassificationContext;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClassificationPromptBuilder {

    public static final String PROMPT_VERSION = "v2";

    private final ObjectMapper objectMapper;

    public ClassificationPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String systemInstructions() {
        return """
                Tu es le moteur de classification d'AlertOps.
                Tu analyses une alerte Dynatrace et tu choisis le candidat métier le plus pertinent parmi ceux fournis.

                RESTRICTIONS ABSOLUES :
                - N'invente jamais une Solution, un Domaine, un Pôle ou une Entité.
                - Utilise uniquement les candidats fournis dans RETRIEVED BUSINESS CONTEXT.
                - Si aucun candidat n'est fiable, retourne fallback=true et matchedSolution=null.
                - Ne calcule pas, ne proposes pas et ne retournes pas de PSI / priorité métier.
                - Ne décide pas du routage, du canal, de l'escalade ni d'un contact humain.
                - Ne retourne aucune donnée d'administrateur ou d'email.

                OUTPUT CONTRACT (JSON strict, sans markdown) :
                {
                  "category": "AVAILABILITY|ERROR|PERFORMANCE|RESOURCE_CONTENTION|SECURITY|CUSTOM_ALERT|UNKNOWN",
                  "problemType": "string court",
                  "confidence": 0.0,
                  "matchedSolution": "nom exact d'un candidat ou null",
                  "matchedDomaine": "string ou null",
                  "matchedPole": "string ou null",
                  "matchedEntity": "string ou null",
                  "summary": "résumé court",
                  "probableCause": "string ou null",
                  "justification": "justification courte",
                  "uncertainFields": [],
                  "requiresHumanValidation": false,
                  "fallback": false
                }
                """;
    }

    public String userPrompt(AlertClassificationContext alert, List<SolutionContext> solutions) {
        try {
            return """
                    ALERT CONTEXT
                    %s

                    RETRIEVED BUSINESS CONTEXT (Top-K candidats PostgreSQL)
                    %s

                    TASK
                    Choisis le candidat métier le plus pertinent pour cette alerte.
                    Si tu n'es pas suffisamment confiant, baisse confidence, renseigne uncertainFields et fallback=true.
                    """.formatted(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(alert),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(solutions)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize classification prompt", e);
        }
    }
}
