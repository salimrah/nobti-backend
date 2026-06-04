package ma.noba.shared.domain.event;

public interface EventBus {

    void publish(DomainEvent event);
}
