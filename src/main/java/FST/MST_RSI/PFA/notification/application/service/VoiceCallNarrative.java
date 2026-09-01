package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentRepository;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.service.SipHangupCauseMapper;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Component
public class VoiceCallNarrative {

    private final PersonRepository personRepository;
    private final UnitAdminAssignmentRepository assignmentRepository;
    private final OrganizationalUnitRepository unitRepository;

    public VoiceCallNarrative(
            PersonRepository personRepository,
            UnitAdminAssignmentRepository assignmentRepository,
            OrganizationalUnitRepository unitRepository
    ) {
        this.personRepository = personRepository;
        this.assignmentRepository = assignmentRepository;
        this.unitRepository = unitRepository;
    }

    public String describe(VoiceCallSessionEntity session, String phase, Integer hangupCause, String causeTxt) {
        RecipientContext recipient = resolveRecipient(session);
        String who = recipient.label();
        boolean answered = session.getAnsweredAt() != null;

        return switch (phase) {
            case "RINGING" -> "Appel VoIP vers " + who + " : sonnerie en cours"
                    + extensionSuffix(session) + ".";
            case "ANSWERED" -> {
                int wait = ringSeconds(session);
                yield "Appel VoIP vers " + who + " répondu"
                        + (wait > 0 ? " après " + wait + " seconde" + (wait > 1 ? "s" : "") : "")
                        + extensionSuffix(session) + ".";
            }
            default -> finishSentence(session, who, hangupCause, causeTxt, answered);
        };
    }

    private String finishSentence(
            VoiceCallSessionEntity session,
            String who,
            Integer hangupCause,
            String causeTxt,
            boolean answered
    ) {
        String cause = SipHangupCauseMapper.describe(hangupCause, answered, causeTxt);
        VoiceCallOutcome outcome = parseOutcome(session.getOutcome());
        int duration = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;
        String source = hangupParty(session.getHangupSource());

        return switch (outcome) {
            case BUSY -> "Appel VoIP vers " + who + " échoué : le destinataire est occupé (" + cause + ").";
            case REJECTED -> "Appel VoIP vers " + who + " refusé par le destinataire (" + cause + ").";
            case NO_ANSWER -> "Appel VoIP vers " + who + " sans réponse (" + cause + ").";
            case FAILED -> "Appel VoIP vers " + who + " échoué : " + cause + ".";
            case HANGUP, ANSWERED -> "Appel VoIP vers " + who + " terminé"
                    + (duration > 0 ? " après " + duration + " seconde" + (duration > 1 ? "s" : "") : "")
                    + (source != null ? " — raccroché par " + source : "")
                    + " (" + cause + ").";
            default -> "Appel VoIP vers " + who + " : " + cause + ".";
        };
    }

    private RecipientContext resolveRecipient(VoiceCallSessionEntity session) {
        if (session.getPersonId() == null) {
            return new RecipientContext("destinataire inconnu", null, null);
        }
        PersonEntity person = personRepository.findById(session.getPersonId()).orElse(null);
        String name = person != null ? person.getFullName() : "destinataire inconnu";
        List<UnitAdminAssignmentEntity> assignments = assignmentRepository.findByPersonIdAndActiveTrue(session.getPersonId());
        UnitAdminAssignmentEntity assignment = pickAssignment(assignments);
        String role = assignment == null ? null : displayRole(assignment.getRole());
        String solution = null;
        if (assignment != null) {
            solution = unitRepository.findById(assignment.getUnitId())
                    .map(OrganizationalUnitEntity::getName)
                    .orElse(null);
        }
        return new RecipientContext(name, role, solution);
    }

    private UnitAdminAssignmentEntity pickAssignment(List<UnitAdminAssignmentEntity> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return null;
        }
        return assignments.stream()
                .sorted(Comparator
                        .comparing((UnitAdminAssignmentEntity a) -> unitRank(a.getUnitId()))
                        .thenComparing(UnitAdminAssignmentEntity::isPrimaryContact, Comparator.reverseOrder()))
                .findFirst()
                .orElse(assignments.getFirst());
    }

    private int unitRank(java.util.UUID unitId) {
        return unitRepository.findById(unitId)
                .map(u -> switch (u.getUnitType()) {
                    case "SOLUTION" -> 0;
                    case "DOMAIN" -> 1;
                    case "POLE" -> 2;
                    case "ENTITY" -> 3;
                    default -> 9;
                })
                .orElse(9);
    }

    private static String displayRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return switch (role.trim().toUpperCase().replace(' ', '_')) {
            case "TAM" -> "TAM";
            case "ADMIN_TECHNIQUE", "ADMINTECHNIQUE", "ADMIN" -> "Administrateur technique";
            case "RESPONSABLE_DOMAINE", "DOMAIN_MANAGER" -> "Responsable domaine";
            case "RESPONSABLE_POLE", "POLE_MANAGER" -> "Responsable pôle";
            case "RESPONSABLE_ENTITE", "DIRECTEUR_ENTITE", "ENTITY_MANAGER" -> "Responsable d'entité";
            default -> role;
        };
    }

    private static String extensionSuffix(VoiceCallSessionEntity session) {
        if (session.getExtension() == null || session.getExtension().isBlank()) {
            return "";
        }
        return " (ext. " + session.getExtension() + ")";
    }

    private static int ringSeconds(VoiceCallSessionEntity session) {
        if (session.getAnsweredAt() == null || session.getStartedAt() == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(session.getStartedAt(), session.getAnsweredAt()).toSeconds());
    }

    private static String hangupParty(String source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case "SUPERVISOR" -> "le superviseur";
            case "OPS" -> "le destinataire";
            default -> null;
        };
    }

    private static VoiceCallOutcome parseOutcome(String outcome) {
        try {
            return VoiceCallOutcome.valueOf(outcome);
        } catch (Exception ex) {
            return VoiceCallOutcome.FAILED;
        }
    }

    private record RecipientContext(String name, String role, String solution) {
        String label() {
            if (role != null && solution != null) {
                return name + " (" + role + " — " + solution + ")";
            }
            if (role != null) {
                return name + " (" + role + ")";
            }
            return name;
        }
    }
}
