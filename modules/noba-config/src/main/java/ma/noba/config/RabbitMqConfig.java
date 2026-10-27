package ma.noba.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "event.bus.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

    @Value("${event.rabbitmq.exchange:noba-events}")
    private String exchangeName;

    @Value("${event.rabbitmq.queue:noba-events-queue}")
    private String queueName;

    @Value("${event.rabbitmq.dlq:noba-events-dlq}")
    private String dlqName;

    @Value("${event.rabbitmq.retry-queue:noba-events-retry}")
    private String retryQueueName;

    @Value("${event.rabbitmq.routing-key:#}")
    private String routingKey;

    @Bean
    public TopicExchange domainEventExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue domainEventQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", dlqName)
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-max-length", 100000)
                .build();
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(retryQueueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", routingKey)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(exchangeName + ".dlx", true, false);
    }

    @Bean
    public Binding domainEventBinding(Queue domainEventQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(domainEventQueue)
                .to(domainEventExchange)
                .with(routingKey);
    }

    @Bean
    public Binding retryBinding(Queue retryQueue, TopicExchange domainEventExchange) {
        return BindingBuilder.bind(retryQueue)
                .to(domainEventExchange)
                .with("retry." + routingKey);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(dlqName);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ publish not confirmed: correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : "null", cause);
            }
        });
        template.setReturnsCallback(returned ->
                log.warn("RabbitMQ message returned: exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText()));
        return template;
    }
}
