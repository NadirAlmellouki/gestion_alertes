package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UnitAdminAssignmentRepository extends JpaRepository<UnitAdminAssignmentEntity, UUID> {

    Optional<UnitAdminAssignmentEntity> findByUnitIdAndPersonIdAndRole(UUID unitId, UUID personId, String role);
}
