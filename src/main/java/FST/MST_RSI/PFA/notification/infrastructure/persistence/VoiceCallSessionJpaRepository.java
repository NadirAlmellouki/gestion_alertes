package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoiceCallSessionJpaRepository extends JpaRepository<VoiceCallSessionEntity, UUID> {

    Optional<VoiceCallSessionEntity> findByProviderCallId(String providerCallId);
}
