package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonContactStateRepository extends JpaRepository<PersonContactStateEntity, UUID> {
}
