package FST.MST_RSI.PFA.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification.voip")
public class VoipNotificationProperties {

    private boolean enabled = false;
    private String provider = "local";
    private String gatewayUrl = "http://localhost:8088/api/v1/calls";
    private String ariUrl = "http://localhost:8088";
    private String ariUser = "alertops";
    private String ariPassword = "alertops";
    private String wsUrl = "ws://localhost:8088/ws";
    private String audioDir = "docker/asterisk/sounds";
    private String sipDomain = "localhost";
    private String sipPassword = "alertops";
    private int timeoutSeconds = 30;
    private int maxAttempts = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getAriUrl() {
        return ariUrl;
    }

    public void setAriUrl(String ariUrl) {
        this.ariUrl = ariUrl;
    }

    public String getAriUser() {
        return ariUser;
    }

    public void setAriUser(String ariUser) {
        this.ariUser = ariUser;
    }

    public String getAriPassword() {
        return ariPassword;
    }

    public void setAriPassword(String ariPassword) {
        this.ariPassword = ariPassword;
    }

    public String getWsUrl() {
        return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
    }

    public String getAudioDir() {
        return audioDir;
    }

    public void setAudioDir(String audioDir) {
        this.audioDir = audioDir;
    }

    public String getSipDomain() {
        return sipDomain;
    }

    public void setSipDomain(String sipDomain) {
        this.sipDomain = sipDomain;
    }

    public String getSipPassword() {
        return sipPassword;
    }

    public void setSipPassword(String sipPassword) {
        this.sipPassword = sipPassword;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
