package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class RoutingPolicyEntity {
    @Id
    @GeneratedValue
    private Long id;
}
