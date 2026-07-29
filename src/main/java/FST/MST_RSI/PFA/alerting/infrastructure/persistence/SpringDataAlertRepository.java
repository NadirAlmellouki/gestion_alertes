package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAlertRepository extends JpaRepository<AlertEntity, Long> {
}
