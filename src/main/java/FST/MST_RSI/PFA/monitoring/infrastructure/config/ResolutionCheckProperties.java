package FST.MST_RSI.PFA.monitoring.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.monitoring.resolution-check")
public class ResolutionCheckProperties {

    private boolean enabled = true;
    private long initialDelaySeconds = 120;
    private long pollingIntervalSeconds = 300;
    private long maxDurationMinutes = 24 * 60;
    private int maxAttempts = 288;
    private long pollIntervalMs = 60_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getInitialDelaySeconds() {
        return initialDelaySeconds;
    }

    public void setInitialDelaySeconds(long initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }

    public long getPollingIntervalSeconds() {
        return pollingIntervalSeconds;
    }

    public void setPollingIntervalSeconds(long pollingIntervalSeconds) {
        this.pollingIntervalSeconds = pollingIntervalSeconds;
    }

    public long getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(long maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }
}
