package ma.noba.queue.infra.adapter;

import ma.noba.queue.domain.model.QueueStatus;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.shared.domain.port.AdminQueuePort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdminQueuePortAdapter implements AdminQueuePort {

    private final QueueRepository queueRepository;

    public AdminQueuePortAdapter(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @Override
    public long countActiveQueues() {
        return queueRepository.countByStatusIn(List.of(QueueStatus.ACTIVE, QueueStatus.PAUSED));
    }

    @Override
    public long countTotalQueueMembers() {
        return queueRepository.countAllMembers();
    }
}
