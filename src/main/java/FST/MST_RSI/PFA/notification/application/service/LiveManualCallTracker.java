package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.notification.application.dto.VoiceCallSessionStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class LiveManualCallTracker {

    private static final Logger log = LoggerFactory.getLogger(LiveManualCallTracker.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<UUID, VoiceCallSessionStatusDto> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void started(VoiceCallSessionStatusDto snapshot) {
        sessions.put(snapshot.sessionId(), snapshot);
        send(snapshot.sessionId(), "started", snapshot);
    }

    public void updated(VoiceCallSessionStatusDto snapshot) {
        sessions.put(snapshot.sessionId(), snapshot);
        send(snapshot.sessionId(), snapshot.active() ? "updated" : "ended", snapshot);
        if (!snapshot.active()) {
            completeEmitters(snapshot.sessionId());
        }
    }

    public Optional<VoiceCallSessionStatusDto> get(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public SseEmitter subscribe(UUID sessionId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(ex -> removeEmitter(sessionId, emitter));
        get(sessionId).ifPresent(snapshot -> {
            try {
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
            } catch (IOException ex) {
                removeEmitter(sessionId, emitter);
            }
        });
        return emitter;
    }

    private void send(UUID sessionId, String event, VoiceCallSessionStatusDto snapshot) {
        List<SseEmitter> list = emitters.get(sessionId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(snapshot));
            } catch (Exception ex) {
                log.debug("SSE send failed for session {}: {}", sessionId, ex.getMessage());
                removeEmitter(sessionId, emitter);
            }
        }
    }

    private void completeEmitters(UUID sessionId) {
        List<SseEmitter> list = emitters.remove(sessionId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    private void removeEmitter(UUID sessionId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(sessionId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    public static VoiceCallSessionStatusDto snapshot(
            UUID sessionId,
            String outcome,
            boolean active,
            String supervisorChannelId,
            String adminChannelId,
            String supervisorExtension,
            Instant startedAt,
            Instant answeredAt,
            Instant endedAt,
            Integer hangupCause
    ) {
        return new VoiceCallSessionStatusDto(
                sessionId,
                outcome,
                active,
                supervisorChannelId,
                adminChannelId,
                supervisorExtension,
                startedAt,
                answeredAt,
                endedAt,
                hangupCause
        );
    }
}
