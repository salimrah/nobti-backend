package ma.noba.queue.application.service;

import ma.noba.queue.domain.event.ClientJoinedQueueEvent;
import ma.noba.queue.domain.event.ClientLeftQueueEvent;
import ma.noba.queue.domain.event.ClientTurnStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QueueWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(QueueWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public QueueWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onClientJoined(ClientJoinedQueueEvent event) {
        String topic = "/topic/queues/" + event.providerId();
        messagingTemplate.convertAndSend(topic, Map.of(
                "event", "client.joined",
                "providerId", event.providerId().toString(),
                "clientId", event.clientId().toString(),
                "position", event.position(),
                "estimatedStartTime", event.estimatedStartTime() != null ? event.estimatedStartTime().toString() : null
        ));
        log.debug("WebSocket notification sent to {}", topic);
    }

    @EventListener
    public void onClientLeft(ClientLeftQueueEvent event) {
        String topic = "/topic/queues/" + event.providerId();
        messagingTemplate.convertAndSend(topic, Map.of(
                "event", "client.left",
                "providerId", event.providerId().toString(),
                "clientId", event.clientId().toString()
        ));
        log.debug("WebSocket notification sent to {}", topic);
    }

    @EventListener
    public void onClientTurnStarted(ClientTurnStartedEvent event) {
        String clientTopic = "/queue/turns/" + event.clientId();
        messagingTemplate.convertAndSend(clientTopic, Map.of(
                "event", "turn.started",
                "providerId", event.providerId().toString(),
                "clientId", event.clientId().toString(),
                "queueId", event.queueId().toString()
        ));
        log.debug("WebSocket turn notification sent to {}", clientTopic);
    }
}
