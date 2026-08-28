package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoiceCallSessionJpaRepository extends JpaRepository<VoiceCallSessionEntity, UUID> {

    Optional<VoiceCallSessionEntity> findByProviderCallId(String providerCallId);

    @Query("""
            SELECT s FROM VoiceCallSessionEntity s
            WHERE s.providerCallId = :channelId
               OR s.supervisorChannelId = :channelId
            """)
    Optional<VoiceCallSessionEntity> findByAnyChannelId(@Param("channelId") String channelId);

    Optional<VoiceCallSessionEntity> findTopByRoutingExecutionIdOrderByStartedAtDesc(UUID routingExecutionId);

    List<VoiceCallSessionEntity> findByRoutingExecutionId(UUID routingExecutionId);
}
