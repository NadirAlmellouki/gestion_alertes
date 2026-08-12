package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, UUID> {

    Optional<OrganizationalUnitEntity> findByUnitTypeAndNameAndParentUnitId(
            String unitType,
            String name,
            UUID parentUnitId
    );

    Optional<OrganizationalUnitEntity> findByUnitTypeAndNameAndParentUnitIdIsNull(String unitType, String name);

    Optional<OrganizationalUnitEntity> findByCode(String code);

    List<OrganizationalUnitEntity> findByUnitTypeAndActiveTrue(String unitType);
}
