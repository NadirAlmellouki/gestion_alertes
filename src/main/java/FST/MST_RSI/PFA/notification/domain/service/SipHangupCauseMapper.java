package FST.MST_RSI.PFA.notification.domain.service;

import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;

public final class SipHangupCauseMapper {

    private SipHangupCauseMapper() {
    }

    public static VoiceCallOutcome fromCause(Integer cause, boolean answered) {
        if (cause == null) {
            return answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.FAILED;
        }
        return switch (cause) {
            case 17 -> VoiceCallOutcome.BUSY;
            case 21, 22 -> VoiceCallOutcome.REJECTED;
            case 18, 19 -> VoiceCallOutcome.NO_ANSWER;
            case 1, 3, 20, 27 -> answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.FAILED;
            case 16, 31 -> answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.NO_ANSWER;
            default -> answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.FAILED;
        };
    }

    /**
     * Q.850 / Asterisk hangup causes. Returns null only when neither a numeric cause
     * nor a cause text was provided by ARI.
     */
    public static String describe(Integer cause, boolean answered, String causeTxt) {
        String fromCode = describeCode(cause, answered);
        if (fromCode != null) {
            return fromCode;
        }
        if (causeTxt != null && !causeTxt.isBlank()) {
            return answered
                    ? "Appel terminé (" + causeTxt.trim() + ")"
                    : "Échec d'appel (" + causeTxt.trim() + ")";
        }
        return answered
                ? "Appel terminé — Asterisk n'a pas fourni de cause SIP (événement sans hangup_cause)"
                : "Échec d'appel — Asterisk n'a pas fourni de cause SIP (événement sans hangup_cause)";
    }

    public static boolean indicatesUnregistered(Integer cause, String errorMessage) {
        if (cause != null && (cause == 20 || cause == 1 || cause == 3)) {
            return true;
        }
        if (errorMessage == null) {
            return false;
        }
        String msg = errorMessage.toLowerCase();
        return msg.contains("allocation failed")
                || msg.contains("not found")
                || msg.contains("offline")
                || msg.contains("unregistered");
    }

    public static boolean indicatesUnreachable(Integer cause) {
        return cause != null && (cause == 27 || cause == 3 || cause == 1 || cause == 20 || cause == 41 || cause == 42);
    }

    private static String describeCode(Integer cause, boolean answered) {
        if (cause == null) {
            return null;
        }
        return switch (cause) {
            case 1 -> "Numéro / extension non alloué (destinataire inconnu du PBX)";
            case 3 -> "Pas d'itinéraire vers le destinataire";
            case 16 -> answered ? "Raccroché normalement (Normal Clearing)" : "Pas de réponse (Normal Clearing avant décroché)";
            case 17 -> "Occupé";
            case 18 -> "Pas de réponse de l'utilisateur";
            case 19 -> "Pas de réponse (timeout de sonnerie)";
            case 20 -> "Abonné absent — extension SIP non enregistrée";
            case 21, 22 -> "Appel refusé par le destinataire";
            case 27 -> "Destination hors service";
            case 28 -> "Format de numéro invalide";
            case 31 -> answered ? "Fin d'appel (cause non spécifiée)" : "Échec (cause non spécifiée)";
            case 34 -> "Aucun circuit / canal disponible";
            case 38 -> "Réseau hors service";
            case 41 -> "Réseau temporairement indisponible";
            default -> (answered ? "Appel terminé" : "Échec d'appel") + " (cause SIP " + cause + ")";
        };
    }
}
