package FST.MST_RSI.PFA.routingengine.domain.model;

import java.util.UUID;

public record ResolvedPerson(
        UUID personId,
        String fullName,
        String email,
        String role,
        UUID unitId,
        boolean primaryContact
) {
}
