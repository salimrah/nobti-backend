package ma.noba.shared.infra.webhook;

import ma.noba.shared.domain.webhook.WebhookDeliveryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

@Component
public class WebhookRetryJob {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryJob.class);
    private static final int MAX_RETRIES = 5;

    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookRetryJob(WebhookDeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @SchedulerLock(name = "webhook-retry", lockAtMostFor = "2m", lockAtLeastFor = "10s")
    @Transactional
    public void retryFailedDeliveries() {
        List<WebhookDeliveryLog> failed = deliveryLogRepository
                .findBySuccessFalseAndNextRetryAtBeforeAndRetryCountLessThan(
                        false, Instant.now(), MAX_RETRIES);

        for (WebhookDeliveryLog logEntry : failed) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> request = new HttpEntity<>(logEntry.getResponseBody(), headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        logEntry.getUrl(), HttpMethod.POST, request, String.class);

                boolean success = response.getStatusCode().is2xxSuccessful();
                logEntry.setSuccess(success);
                logEntry.setStatusCode(response.getStatusCodeValue());
                logEntry.setResponseBody(response.getBody());
                logEntry.setRetryCount(logEntry.getRetryCount() + 1);
                logEntry.setNextRetryAt(success ? null : nextRetryTime(logEntry.getRetryCount()));
                deliveryLogRepository.save(logEntry);

                if (success) {
                    log.info("Webhook retry succeeded for {}", logEntry.getUrl());
                }
            } catch (RestClientException e) {
                logEntry.setRetryCount(logEntry.getRetryCount() + 1);
                logEntry.setNextRetryAt(nextRetryTime(logEntry.getRetryCount()));
                logEntry.setErrorMessage(e.getMessage());
                deliveryLogRepository.save(logEntry);
                log.warn("Webhook retry failed for {} (attempt {}/{}): {}",
                        logEntry.getUrl(), logEntry.getRetryCount(), MAX_RETRIES, e.getMessage());
            }
        }
    }

    private Instant nextRetryTime(int retryCount) {
        long delayMinutes = (long) Math.pow(2, retryCount) * 5;
        return Instant.now().plusSeconds(delayMinutes * 60);
    }
}
