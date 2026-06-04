package ma.noba.shared.domain.port;

import ma.noba.shared.domain.model.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);
}
