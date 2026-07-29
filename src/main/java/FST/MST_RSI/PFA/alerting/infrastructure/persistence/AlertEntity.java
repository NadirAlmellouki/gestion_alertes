package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class AlertEntity {
    @Id
    @GeneratedValue
    private Long id;
}
