package FST.MST_RSI.PFA.routingengine.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RoutingEscalationProperties.class)
public class RoutingEngineConfig {
}
