package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByCodeAndActiveTrue(String code);
}
