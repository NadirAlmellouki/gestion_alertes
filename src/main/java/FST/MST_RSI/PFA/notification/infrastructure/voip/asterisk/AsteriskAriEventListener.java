package FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk;

import FST.MST_RSI.PFA.notification.application.usecase.ApplyVoiceCallOutcomeUseCase;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.service.SipHangupCauseMapper;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
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
import java.util.Set;
import java.util.UUID;
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

    private final Map<String, Boolean> answered = new ConcurrentHashMap<>();
    private final Map<String, String> sounds = new ConcurrentHashMap<>();
    private final Map<String, Boolean> live = new ConcurrentHashMap<>();

    private final Map<String, String> channelToSession = new ConcurrentHashMap<>();
    private final Map<String, String[]> sessionChannels = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionBridged = new ConcurrentHashMap<>();
    private final Map<String, String> sessionBridgeId = new ConcurrentHashMap<>();
    private final Set<String> tearingDown = ConcurrentHashMap.newKeySet();

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

    public void registerBridgeSession(String sessionId, String supervisorChannelId, String adminChannelId) {
        sessionChannels.put(sessionId, new String[]{supervisorChannelId, adminChannelId});
        channelToSession.put(supervisorChannelId, sessionId);
        channelToSession.put(adminChannelId, sessionId);
        sessionBridged.put(sessionId, false);
        tearingDown.remove(sessionId);
        log.info("[VOICE] Registered bridge session={} supervisor={} admin={}", sessionId, supervisorChannelId, adminChannelId);
    }

    public void terminateSession(String sessionId) {
        teardownSession(sessionId, null, "SUPERVISOR");
    }

    public void terminateByChannel(String channelId) {
        String sessionId = resolveSessionId(channelId);
        teardownSession(sessionId, channelId, "SUPERVISOR");
        if (sessionId == null) {
            ariClient.hangup(channelId);
        }
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

        if ("BridgeDestroyed".equals(type)) {
            String bridgeId = event.path("bridge").path("id").asText(null);
            if (bridgeId != null) {
                sessionBridgeId.entrySet().removeIf(e -> bridgeId.equals(e.getValue()));
            }
            return;
        }

        if (channelId == null || channelId.isBlank()) {
            return;
        }
        switch (type) {
            case "StasisStart" -> onStasisStart(event, channelId);
            case "ChannelStateChange" -> onStateChange(channel, channelId);
            case "PlaybackFinished" -> {
                if (!Boolean.TRUE.equals(live.get(channelId))) {
                    ariClient.hangup(channelId);
                }
            }
            case "ChannelDestroyed", "ChannelHangupRequest" -> onHangup(event, channel, channelId);
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
        String sessionFromArgs = parsed.get("session");
        if (sessionFromArgs != null && !sessionFromArgs.isBlank()) {
            channelToSession.putIfAbsent(channelId, sessionFromArgs);
        }

        try {
            ariClient.answer(channelId);
            answered.put(channelId, true);
            applyVoiceCallOutcomeUseCase.answered(channelId);

            String sound = sounds.get(channelId);
            boolean isLive = Boolean.TRUE.equals(live.get(channelId));

            if (isLive) {
                String sessionId = channelToSession.get(channelId);
                if (sessionId != null) {
                    tryBridgeSession(sessionId);
                } else {
                    ariClient.record(channelId, "manual-" + channelId);
                }
            } else if (sound != null && !sound.isBlank()) {
                ariClient.play(channelId, sound);
            }
        } catch (Exception ex) {
            log.warn("[VOICE] StasisStart handling failed for {}: {}", channelId, ex.getMessage());
        }
    }

    private synchronized void tryBridgeSession(String sessionId) {
        if (Boolean.TRUE.equals(sessionBridged.get(sessionId))) {
            return;
        }
        String[] channels = sessionChannels.get(sessionId);
        if (channels == null || channels.length < 2) {
            return;
        }

        String supervisorCh = channels[0];
        String adminCh = channels[1];

        boolean supervisorReady = Boolean.TRUE.equals(answered.get(supervisorCh));
        boolean adminReady = Boolean.TRUE.equals(answered.get(adminCh));

        if (!supervisorReady || !adminReady) {
            log.info("[VOICE] Bridge session={} waiting — supervisor={} admin={}", sessionId, supervisorReady, adminReady);
            return;
        }

        log.info("[VOICE] Both channels ready for session={}, creating bridge...", sessionId);
        String bridgeId = ariClient.bridge(supervisorCh, adminCh);
        if (bridgeId != null) {
            sessionBridgeId.put(sessionId, bridgeId);
            sessionBridged.put(sessionId, true);
            String recName = "manual-" + sessionId;
            ariClient.recordBridge(bridgeId, recName);
            try {
                applyVoiceCallOutcomeUseCase.attachBridge(UUID.fromString(sessionId), bridgeId, recName);
            } catch (IllegalArgumentException ignored) {
            }
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
            String sessionId = channelToSession.get(channelId);
            if (sessionId != null && Boolean.TRUE.equals(live.get(channelId))) {
                tryBridgeSession(sessionId);
            }
        }
    }

    private void onHangup(JsonNode event, JsonNode channel, String channelId) {
        Integer cause = extractCause(event, channel, channelId);
        String causeTxt = extractCauseText(event, channel);
        String sessionId = resolveSessionId(channelId);
        String hangupSource = hangupSource(sessionId, channelId);
        teardownSession(sessionId, channelId, hangupSource);

        boolean wasAnswered = Boolean.TRUE.equals(answered.get(channelId));
        VoiceCallOutcome outcome = SipHangupCauseMapper.fromCause(cause, wasAnswered);
        applyVoiceCallOutcomeUseCase.finished(channelId, outcome, cause, hangupSource, causeTxt);
        answered.remove(channelId);
        sounds.remove(channelId);
        live.remove(channelId);
    }

    public void teardownSession(String sessionId, String originatingChannelId, String hangupSource) {
        if (sessionId == null || !tearingDown.add(sessionId)) {
            if (sessionId == null && originatingChannelId != null) {
                ariClient.hangup(originatingChannelId);
            }
            return;
        }
        try {
            VoiceCallSessionEntity dbSession = null;
            try {
                dbSession = sessionRepository.findById(UUID.fromString(sessionId)).orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
            if (dbSession == null && originatingChannelId != null) {
                dbSession = sessionRepository.findByAnyChannelId(originatingChannelId).orElse(null);
            }

            String bridgeId = sessionBridgeId.remove(sessionId);
            if (bridgeId == null && dbSession != null) {
                bridgeId = dbSession.getBridgeId();
            }
            String recordingName = dbSession != null && dbSession.getRecordingName() != null
                    ? dbSession.getRecordingName()
                    : "manual-" + sessionId;
            if (bridgeId != null) {
                ariClient.stopRecording(recordingName);
                ariClient.destroyBridge(bridgeId);
            }

            String[] channels = sessionChannels.remove(sessionId);
            if (channels == null && dbSession != null) {
                channels = new String[]{dbSession.getSupervisorChannelId(), dbSession.getProviderCallId()};
            }
            if (channels != null) {
                for (String ch : channels) {
                    if (ch == null || ch.isBlank()) {
                        continue;
                    }
                    channelToSession.remove(ch);
                    if (!ch.equals(originatingChannelId)) {
                        log.info("[VOICE] Hanging up peer channel {} after {} hangup session={}", ch, hangupSource, sessionId);
                        ariClient.hangup(ch);
                    }
                }
            }
            sessionBridged.remove(sessionId);
        } finally {
            log.info("[VOICE] Session teardown complete session={} source={}", sessionId, hangupSource);
        }
    }

    private String resolveSessionId(String channelId) {
        String sessionId = channelToSession.get(channelId);
        if (sessionId != null) {
            return sessionId;
        }
        return sessionRepository.findByAnyChannelId(channelId)
                .map(s -> s.getId().toString())
                .orElse(null);
    }

    private String hangupSource(String sessionId, String channelId) {
        VoiceCallSessionEntity db = null;
        try {
            if (sessionId != null) {
                db = sessionRepository.findById(UUID.fromString(sessionId)).orElse(null);
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (db == null) {
            db = sessionRepository.findByAnyChannelId(channelId).orElse(null);
        }
        if (db == null) {
            return "UNKNOWN";
        }
        if (channelId.equals(db.getSupervisorChannelId())) {
            return "SUPERVISOR";
        }
        if (channelId.equals(db.getProviderCallId())) {
            return "OPS";
        }
        return "UNKNOWN";
    }

    private Integer extractCause(JsonNode event, JsonNode channel, String channelId) {
        if (event != null && event.has("cause") && event.path("cause").canConvertToInt()) {
            return event.path("cause").asInt();
        }
        if (event != null && event.has("hangup_cause") && event.path("hangup_cause").canConvertToInt()) {
            return event.path("hangup_cause").asInt();
        }
        if (channel.has("hangup_cause") && channel.path("hangup_cause").canConvertToInt()) {
            return channel.path("hangup_cause").asInt();
        }
        if (channel.has("cause") && channel.path("cause").canConvertToInt()) {
            return channel.path("cause").asInt();
        }
        return sessionRepository.findByAnyChannelId(channelId)
                .map(VoiceCallSessionEntity::getHangupCause)
                .orElse(null);
    }

    private static String extractCauseText(JsonNode event, JsonNode channel) {
        String txt = event != null ? event.path("cause_txt").asText(null) : null;
        if (txt == null || txt.isBlank()) {
            txt = channel.path("cause_txt").asText(null);
        }
        if (txt == null || txt.isBlank()) {
            txt = channel.path("hangup_cause_txt").asText(null);
        }
        return txt != null && !txt.isBlank() ? txt : null;
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
