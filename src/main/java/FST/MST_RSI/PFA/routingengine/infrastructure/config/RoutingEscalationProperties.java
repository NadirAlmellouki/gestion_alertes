package FST.MST_RSI.PFA.routingengine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.routing.escalation")
public class RoutingEscalationProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 30_000;
    private int maxActiveMinutes = 24 * 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getMaxActiveMinutes() {
        return maxActiveMinutes;
    }

    public void setMaxActiveMinutes(int maxActiveMinutes) {
        this.maxActiveMinutes = maxActiveMinutes;
    }
}
