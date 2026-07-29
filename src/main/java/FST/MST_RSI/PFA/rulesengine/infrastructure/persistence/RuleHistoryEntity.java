package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class RuleHistoryEntity {
    @Id
    @GeneratedValue
    private Long id;
}
