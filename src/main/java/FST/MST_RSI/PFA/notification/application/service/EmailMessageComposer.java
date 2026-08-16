package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.NotificationTemplateEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmailMessageComposer {

    public String renderSubject(NotificationTemplateEntity template, Alert alert, ClassificationResult classification) {
        return render(template.getTitle(), alert, classification, null);
    }

    public String renderBody(
            NotificationTemplateEntity template,
            Alert alert,
            ClassificationResult classification,
            String recipientName
    ) {
        return render(template.getBody(), alert, classification, recipientName);
    }

    private String render(
            String template,
            Alert alert,
            ClassificationResult classification,
            String recipientName
    ) {
        if (template == null) {
            return "";
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("recipientName", recipientName != null ? recipientName : "collègue");
        values.put("problemId", alert.getExternalProblemId());
        values.put("title", nullToEmpty(alert.getTitle()));
        values.put("severity", nullToEmpty(alert.getSeverity()));
        values.put("impact", nullToEmpty(alert.getImpact()));
        values.put("application", nullToEmpty(alert.getApplicationName()));
        values.put("environment", nullToEmpty(alert.getEnvironment()));
        values.put("solution", classification != null ? nullToEmpty(classification.matchedSolution()) : "");
        values.put("category", classification != null && classification.category() != null
                ? classification.category().name() : "");
        values.put("confidence", classification != null && classification.confidence() != null
                ? String.format("%.0f%%", classification.confidence().value() * 100) : "");

        String rendered = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
