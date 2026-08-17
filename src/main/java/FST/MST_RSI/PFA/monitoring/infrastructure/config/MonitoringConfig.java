package FST.MST_RSI.PFA.monitoring.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ResolutionCheckProperties.class, DynatraceApiProperties.class})
public class MonitoringConfig {
}
