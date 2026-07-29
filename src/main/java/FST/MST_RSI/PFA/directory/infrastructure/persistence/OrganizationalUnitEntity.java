package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class OrganizationalUnitEntity {
    @Id
    @GeneratedValue
    private Long id;
}
