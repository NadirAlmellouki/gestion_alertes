package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonContactStateEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonContactStateRepository;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.service.SipHangupCauseMapper;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RecordPersonVoipContactUseCase {

    private final PersonContactStateRepository contactStateRepository;

    public RecordPersonVoipContactUseCase(PersonContactStateRepository contactStateRepository) {
        this.contactStateRepository = contactStateRepository;
    }

    @Transactional
    public void recordAnswered(VoiceCallSessionEntity session) {
        if (session.getPersonId() == null) {
            return;
        }
        Instant at = session.getAnsweredAt() != null ? session.getAnsweredAt() : Instant.now();
        PersonContactStateEntity state = load(session.getPersonId());
        state.setLastContactAt(at);
        state.setLastVoipAt(at);
        state.setLastVoipOutcome(VoiceCallOutcome.ANSWERED.name());
        state.setLastSuccessAt(at);
        state.setSipReachability("AVAILABLE");
        state.setVoipAnsweredCount(state.getVoipAnsweredCount() + 1);
        state.setUpdatedAt(at);
        contactStateRepository.save(state);
    }

    @Transactional
    public void recordFinished(VoiceCallSessionEntity session) {
        if (session.getPersonId() == null) {
            return;
        }
        Instant at = session.getEndedAt() != null ? session.getEndedAt() : Instant.now();
        boolean answered = session.getAnsweredAt() != null;
        PersonContactStateEntity state = load(session.getPersonId());
        state.setLastContactAt(at);
        state.setLastVoipAt(at);
        state.setLastVoipOutcome(session.getOutcome());
        state.setLastVoipHangupCause(session.getHangupCause());
        state.setUpdatedAt(at);
        if (answered) {
            state.setLastSuccessAt(at);
            state.setSipReachability("AVAILABLE");
        } else {
            state.setLastFailureAt(at);
            state.setVoipFailedCount(state.getVoipFailedCount() + 1);
            state.setSipReachability(reachabilityForFailure(session.getOutcome(), session.getHangupCause(), session.getFailureReason()));
        }
        contactStateRepository.save(state);
    }

    @Transactional
    public void recordOriginateFailure(UUID personId, String errorMessage) {
        if (personId == null) {
            return;
        }
        Instant at = Instant.now();
        PersonContactStateEntity state = load(personId);
        state.setLastContactAt(at);
        state.setLastVoipAt(at);
        state.setLastVoipOutcome(VoiceCallOutcome.FAILED.name());
        state.setLastFailureAt(at);
        state.setVoipFailedCount(state.getVoipFailedCount() + 1);
        state.setSipReachability(SipHangupCauseMapper.indicatesUnregistered(null, errorMessage)
                ? "UNREGISTERED"
                : "UNREACHABLE");
        state.setUpdatedAt(at);
        contactStateRepository.save(state);
    }

    public static boolean shouldSkipAutoVoip(String sipReachability) {
        return "UNREGISTERED".equals(sipReachability) || "UNREACHABLE".equals(sipReachability);
    }

    private PersonContactStateEntity load(UUID personId) {
        return contactStateRepository.findById(personId)
                .orElseGet(() -> PersonContactStateEntity.create(personId));
    }

    private static String reachabilityForFailure(String outcome, Integer cause, String error) {
        if (VoiceCallOutcome.BUSY.name().equals(outcome)) {
            return "BUSY";
        }
        if (VoiceCallOutcome.NO_ANSWER.name().equals(outcome)) {
            return "NO_ANSWER";
        }
        if (VoiceCallOutcome.REJECTED.name().equals(outcome)) {
            return "AVAILABLE";
        }
        if (SipHangupCauseMapper.indicatesUnregistered(cause, error)) {
            return "UNREGISTERED";
        }
        return "UNREACHABLE";
    }
}
