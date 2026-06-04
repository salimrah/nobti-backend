package ma.noba.shared.infra.event;

import ma.noba.shared.domain.event.DomainEvent;
import ma.noba.shared.domain.event.EventBus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventBus implements EventBus {

    private final ApplicationEventPublisher publisher;

    public SpringEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
