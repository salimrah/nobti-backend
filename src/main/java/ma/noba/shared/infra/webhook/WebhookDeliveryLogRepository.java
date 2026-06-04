package ma.noba.shared.infra.webhook;

import ma.noba.shared.domain.webhook.WebhookDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {

    @Query("SELECT w FROM WebhookDeliveryLog w WHERE w.success = :success AND w.nextRetryAt < :before AND w.retryCount < :maxRetries")
    List<WebhookDeliveryLog> findBySuccessFalseAndNextRetryAtBeforeAndRetryCountLessThan(
            @Param("success") boolean success,
            @Param("before") Instant before,
            @Param("maxRetries") int maxRetries);
}
