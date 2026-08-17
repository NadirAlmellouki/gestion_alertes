package FST.MST_RSI.PFA;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "FST.MST_RSI.PFA.alerting.infrastructure.persistence",
        "FST.MST_RSI.PFA.directory.infrastructure.persistence",
        "FST.MST_RSI.PFA.classification.infrastructure.persistence",
        "FST.MST_RSI.PFA.rulesengine.infrastructure.persistence",
        "FST.MST_RSI.PFA.routingengine.infrastructure.persistence",
        "FST.MST_RSI.PFA.notification.infrastructure.persistence",
        "FST.MST_RSI.PFA.monitoring.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
        "FST.MST_RSI.PFA.alerting.infrastructure.persistence",
        "FST.MST_RSI.PFA.directory.infrastructure.persistence",
        "FST.MST_RSI.PFA.classification.infrastructure.persistence",
        "FST.MST_RSI.PFA.rulesengine.infrastructure.persistence",
        "FST.MST_RSI.PFA.routingengine.infrastructure.persistence",
        "FST.MST_RSI.PFA.notification.infrastructure.persistence",
        "FST.MST_RSI.PFA.monitoring.infrastructure.persistence"
})
public class AlertOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertOpsApplication.class, args);
    }
}
