package FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk;

import FST.MST_RSI.PFA.notification.application.usecase.ApplyVoiceCallOutcomeUseCase;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.service.SipHangupCauseMapper;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnExpression("${app.notification.voip.enabled:false} && '${app.notification.voip.provider:local}'.equals('asterisk')")
public class AsteriskAriEventListener extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AsteriskAriEventListener.class);

    private final AsteriskAriClient ariClient;
    private final ApplyVoiceCallOutcomeUseCase applyVoiceCallOutcomeUseCase;
    private final VoiceCallSessionJpaRepository sessionRepository;
    private final ObjectMapper objectMapper;
    private final VoipNotificationProperties properties;

    // Channel state maps
    private final Map<String, Boolean> answered = new ConcurrentHashMap<>();
    private final Map<String, String> sounds = new ConcurrentHashMap<>();
    private final Map<String, Boolean> live = new ConcurrentHashMap<>();

    // Bridge session tracking: sessionId -> [supervisorChannelId, adminChannelId]
    // When both channels are answered, we bridge them.
    private final Map<String, String> channelToSession = new ConcurrentHashMap<>();
    private final Map<String, String[]> sessionChannels = new ConcurrentHashMap<>();  // sessionId -> {ch1, ch2}
    private final Map<String, Boolean> sessionBridged = new ConcurrentHashMap<>();
    private final Map<String, String> sessionBridgeId = new ConcurrentHashMap<>();

    private final ScheduledExecutorService reconnect = Executors.newSingleThreadScheduledExecutor();
    private volatile WebSocketSession session;

    public AsteriskAriEventListener(
            AsteriskAriClient ariClient,
            ApplyVoiceCallOutcomeUseCase applyVoiceCallOutcomeUseCase,
            VoiceCallSessionJpaRepository sessionRepository,
            ObjectMapper objectMapper,
            VoipNotificationProperties properties
    ) {
        this.ariClient = ariClient;
        this.applyVoiceCallOutcomeUseCase = applyVoiceCallOutcomeUseCase;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void connect() {
        reconnect.schedule(this::open, 3, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        reconnect.shutdownNow();
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Called by PlaceManualCallUseCase BEFORE originating to register the pair
     * of channels that should be bridged together when both answer.
     */
    public void registerBridgeSession(String sessionId, String supervisorChannelId, String adminChannelId) {
        sessionChannels.put(sessionId, new String[]{supervisorChannelId, adminChannelId});
        channelToSession.put(supervisorChannelId, sessionId);
        channelToSession.put(adminChannelId, sessionId);
        sessionBridged.put(sessionId, false);
        log.info("[VOICE] Registered bridge session={} supervisor={} admin={}", sessionId, supervisorChannelId, adminChannelId);
    }

    private void open() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            String basic = Base64.getEncoder().encodeToString(
                    (properties.getAriUser() + ":" + properties.getAriPassword()).getBytes(StandardCharsets.UTF_8)
            );
            headers.add("Authorization", "Basic " + basic);
            session = client.execute(this, headers, URI.create(ariClient.eventsWebSocketUrl())).get(8, TimeUnit.SECONDS);
            log.info("[VOICE] Connected to Asterisk ARI events");
        } catch (Exception ex) {
            log.warn("[VOICE] ARI events connection failed: {} — retry in 5s", ex.getMessage());
            reconnect.schedule(this::open, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("[VOICE] ARI websocket closed: {}", status);
        reconnect.schedule(this::open, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode event = objectMapper.readTree(message.getPayload());
        String type = event.path("type").asText();
        JsonNode channel = event.path("channel");
        String channelId = channel.path("id").asText(null);
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        switch (type) {
            case "StasisStart" -> onStasisStart(event, channelId);
            case "ChannelStateChange" -> onStateChange(channel, channelId);
            case "PlaybackFinished" -> ariClient.hangup(channelId);
            case "ChannelDestroyed", "ChannelHangupRequest" -> onHangup(channel, channelId);
            default -> {
            }
        }
    }

    private void onStasisStart(JsonNode event, String channelId) {
        String args = event.path("args").isArray() && event.path("args").size() > 0
                ? event.path("args").get(0).asText("")
                : "";
        Map<String, String> parsed = parseArgs(args);
        sounds.put(channelId, parsed.getOrDefault("sound", ""));
        live.put(channelId, Boolean.parseBoolean(parsed.getOrDefault("live", "false")));

        try {
            ariClient.answer(channelId);
            answered.put(channelId, true);
            applyVoiceCallOutcomeUseCase.answered(channelId);

            String sound = sounds.get(channelId);
            boolean isLive = Boolean.TRUE.equals(live.get(channelId));

            if (isLive) {
                // Check if this channel is part of a bridge session
                String sessionId = channelToSession.get(channelId);
                if (sessionId != null) {
                    tryBridgeSession(sessionId);
                } else {
                    // Standalone live call (no bridge partner) — record individually
                    ariClient.record(channelId, "manual-" + channelId);
                }
            } else if (sound != null && !sound.isBlank()) {
                // Automated TTS playback
                ariClient.play(channelId, sound);
            }
        } catch (Exception ex) {
            log.warn("[VOICE] StasisStart handling failed for {}: {}", channelId, ex.getMessage());
        }
    }

    /**
     * Attempts to bridge a session when both channels have answered.
     * This is safe to call multiple times — only acts once (when both are ready).
     */
    private synchronized void tryBridgeSession(String sessionId) {
        if (Boolean.TRUE.equals(sessionBridged.get(sessionId))) {
            return; // already bridged
        }
        String[] channels = sessionChannels.get(sessionId);
        if (channels == null || channels.length < 2) return;

        String supervisorCh = channels[0];
        String adminCh = channels[1];

        boolean supervisorReady = Boolean.TRUE.equals(answered.get(supervisorCh));
        boolean adminReady = Boolean.TRUE.equals(answered.get(adminCh));

        if (!supervisorReady || !adminReady) {
            log.info("[VOICE] Bridge session={} waiting — supervisor={} admin={}", sessionId, supervisorReady, adminReady);
            return;
        }

        // Both channels answered — create bridge
        log.info("[VOICE] Both channels ready for session={}, creating bridge...", sessionId);
        String bridgeId = ariClient.bridge(supervisorCh, adminCh);
        if (bridgeId != null) {
            sessionBridgeId.put(sessionId, bridgeId);
            sessionBridged.put(sessionId, true);
            // Record the bridge (captures both sides mixed)
            String recName = "manual-" + sessionId;
            ariClient.recordBridge(bridgeId, recName);
            log.info("[VOICE] Bridge active session={} bridgeId={} recording={}", sessionId, bridgeId, recName);
        }
    }

    private void onStateChange(JsonNode channel, String channelId) {
        String state = channel.path("state").asText("");
        log.info("[VOICE] SIP state={} callId={}", state, channelId);
        if ("Ringing".equalsIgnoreCase(state) || "Ring".equalsIgnoreCase(state)) {
            applyVoiceCallOutcomeUseCase.ringing(channelId);
        }
        if ("Up".equalsIgnoreCase(state)) {
            answered.put(channelId, true);
            applyVoiceCallOutcomeUseCase.answered(channelId);
        }
    }

    private void onHangup(JsonNode channel, String channelId) {
        Integer cause = null;
        if (channel.has("hangup_cause") && channel.path("hangup_cause").canConvertToInt()) {
            cause = channel.path("hangup_cause").asInt();
        } else if (channel.has("cause") && channel.path("cause").canConvertToInt()) {
            cause = channel.path("cause").asInt();
        } else {
            var sessionOpt = sessionRepository.findByProviderCallId(channelId);
            if (sessionOpt.isPresent()) {
                cause = sessionOpt.get().getHangupCause();
            }
        }

        // If this channel is part of a bridge session, tear down the bridge
        String sessionId = channelToSession.remove(channelId);
        if (sessionId != null) {
            String bridgeId = sessionBridgeId.remove(sessionId);
            if (bridgeId != null) {
                ariClient.stopRecording("manual-" + sessionId);
                ariClient.destroyBridge(bridgeId);
            }
            // Also hangup the other channel in the session
            String[] channels = sessionChannels.remove(sessionId);
            if (channels != null) {
                for (String ch : channels) {
                    if (!ch.equals(channelId)) {
                        channelToSession.remove(ch);
                        ariClient.hangup(ch);
                    }
                }
            }
            sessionBridged.remove(sessionId);
        }

        boolean wasAnswered = Boolean.TRUE.equals(answered.get(channelId));
        VoiceCallOutcome outcome = SipHangupCauseMapper.fromCause(cause, wasAnswered);
        applyVoiceCallOutcomeUseCase.finished(channelId, outcome, cause);
        answered.remove(channelId);
        sounds.remove(channelId);
        live.remove(channelId);
    }

    private static Map<String, String> parseArgs(String args) {
        Map<String, String> map = new ConcurrentHashMap<>();
        if (args == null || args.isBlank()) {
            return map;
        }
        for (String part : args.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                map.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return map;
    }
}
