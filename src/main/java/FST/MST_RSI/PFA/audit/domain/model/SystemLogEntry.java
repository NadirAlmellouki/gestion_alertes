package FST.MST_RSI.PFA.audit.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_log_entries")
public class SystemLogEntry {
    @Id
    @GeneratedValue
    private Long id;
}
