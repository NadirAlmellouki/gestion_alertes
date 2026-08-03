package FST.MST_RSI.PFA.alerting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ingestion.dynatrace")
public record DynatraceIngestionProperties(String token) {
}
