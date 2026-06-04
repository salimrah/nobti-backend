package ma.noba.shared.infra.webhook;

import ma.noba.shared.domain.webhook.WebhookDeliveryLog;
import ma.noba.shared.domain.webhook.WebhookSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final WebhookSubscriptionRepository repository;
    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookDispatcher(WebhookSubscriptionRepository repository,
                             WebhookDeliveryLogRepository deliveryLogRepository) {
        this.repository = repository;
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Async("domainEvents")
    public void dispatch(String eventType, Map<String, Object> payload) {
        List<WebhookSubscription> subscriptions = repository.findByEventTypeAndIsActiveTrue(eventType);
        
        for (WebhookSubscription sub : subscriptions) {
            try {
                sendWebhook(sub, eventType, payload);
            } catch (Exception e) {
                log.error("Failed to dispatch webhook to {} for event {}: {}", 
                        sub.getUrl(), eventType, e.getMessage());
            }
        }
    }

    private void sendWebhook(WebhookSubscription subscription, String eventType, Map<String, Object> payload) {
        String jsonBody = toJson(payload);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Event", eventType);
        headers.set("X-Webhook-Timestamp", String.valueOf(System.currentTimeMillis()));
        
        if (subscription.getSecret() != null && !subscription.getSecret().isBlank()) {
            String signature = computeSignature(jsonBody, subscription.getSecret());
            headers.set("X-Webhook-Signature", signature);
        }

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    subscription.getUrl(), HttpMethod.POST, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();

            WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog();
            deliveryLog.setSubscriptionId(subscription.getId());
            deliveryLog.setEventType(eventType);
            deliveryLog.setUrl(subscription.getUrl());
            deliveryLog.setStatusCode(response.getStatusCodeValue());
            deliveryLog.setResponseBody(response.getBody());
            deliveryLog.setSuccess(success);
            deliveryLogRepository.save(deliveryLog);

            if (success) {
                log.debug("Webhook dispatched successfully to {} for {}", subscription.getUrl(), eventType);
            } else {
                log.warn("Webhook failed for {} with status {}: {}",
                        subscription.getUrl(), response.getStatusCodeValue(), response.getBody());
            }
        } catch (RestClientException e) {
            log.error("Webhook dispatch failed for {}: {}", subscription.getUrl(), e.getMessage());

            WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog();
            deliveryLog.setSubscriptionId(subscription.getId());
            deliveryLog.setEventType(eventType);
            deliveryLog.setUrl(subscription.getUrl());
            deliveryLog.setStatusCode(0);
            deliveryLog.setSuccess(false);
            deliveryLog.setErrorMessage(e.getMessage());
            deliveryLogRepository.save(deliveryLog);
        }
    }

    private String computeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute webhook signature", e);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize webhook payload", e);
        }
    }
}
