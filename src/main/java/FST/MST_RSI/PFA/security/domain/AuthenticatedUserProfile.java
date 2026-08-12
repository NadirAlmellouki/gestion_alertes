package FST.MST_RSI.PFA.security.domain;

public record AuthenticatedUserProfile(String lastName, String firstName, Role applicationRole) {

    public String displayRole() {
        return applicationRole != null ? applicationRole.name() : "INCONNU";
    }
}
