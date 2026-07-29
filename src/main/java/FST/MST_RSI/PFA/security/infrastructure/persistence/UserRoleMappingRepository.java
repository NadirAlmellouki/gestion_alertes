package FST.MST_RSI.PFA.security.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleMappingRepository extends JpaRepository<UserRoleMappingEntity, Long> {

    Optional<UserRoleMappingEntity> findByUsername(String username);
}
