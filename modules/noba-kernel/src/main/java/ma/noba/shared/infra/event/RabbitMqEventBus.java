package ma.noba.shared.infra.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "event.bus.rabbitmq.enabled", havingValue = "true")
public class RabbitMqEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventBus.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Value("${event.rabbitmq.exchange:noba-events}")
    private String exchange;

    public RabbitMqEventBus(RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper,
                            CircuitBreakerRegistry circuitBreakerRegistry,
                            RetryRegistry retryRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("rabbitmq-publish");
        this.retry = retryRegistry.retry("rabbitmq-publish");
    }

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            MessageProperties props = new MessageProperties();
            props.setContentType("application/json");
            props.setHeader("X-Event-Type", event.eventType());
            props.setHeader("X-Event-Id", event.eventId().toString());
            props.setMessageId(event.eventId().toString());
            props.setExpiration("60000");

            Message message = MessageBuilder.withBody(payload.getBytes())
                    .andProperties(props)
                    .build();

            circuitBreaker.executeRunnable(() ->
                    retry.executeRunnable(() -> {
                        rabbitTemplate.send(exchange, event.eventType(), message);
                        log.debug("Published event {} (id={}) to RabbitMQ", event.eventType(), event.eventId());
                    })
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.eventType(), e);
        } catch (Exception e) {
            log.error("Failed to publish event {} to RabbitMQ after circuit breaker/retry", event.eventType(), e);
        }
    }
}
