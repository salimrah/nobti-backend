package ma.noba.queue.infra.adapter;

import ma.noba.queue.application.service.QueueApplicationService;
import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueueStatus;
import ma.noba.queue.domain.port.service.QueueRepository;
import ma.noba.shared.application.dto.PagedResponse;
import ma.noba.shared.domain.port.AdminQueuePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Component
public class AdminQueuePortAdapter implements AdminQueuePort {

    private final QueueRepository queueRepository;
    private final QueueApplicationService queueService;

    public AdminQueuePortAdapter(QueueRepository queueRepository, QueueApplicationService queueService) {
        this.queueRepository = queueRepository;
        this.queueService = queueService;
    }

    @Override
    public long countActiveQueues() {
        return queueRepository.countByStatusIn(List.of(QueueStatus.ACTIVE, QueueStatus.PAUSED));
    }

    @Override
    public long countTotalQueueMembers() {
        return queueRepository.countAllMembers();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<QueueRow> searchQueues(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        QueueStatus parsed = parseStatus(status);
        Page<Queue> result = parsed == null
                ? queueRepository.findAllByOrderByUpdatedAtDesc(pageable)
                : queueRepository.findByStatusOrderByUpdatedAtDesc(parsed, pageable);
        return PagedResponse.of(
                result.getContent().stream().map(AdminQueuePortAdapter::toRow).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QueueDetail> queueDetail(UUID queueId) {
        return queueRepository.findById(queueId).map(queue -> new QueueDetail(
                queue.getId(),
                queue.getProviderId(),
                queue.getStatus().name(),
                queue.getCreatedAt(),
                queue.getUpdatedAt(),
                queue.getMembers().stream()
                        .filter(member -> member.getStatus() == QueueMemberStatus.WAITING
                                || member.getStatus() == QueueMemberStatus.NOTIFIED
                                || member.getStatus() == QueueMemberStatus.ACTIVE)
                        .sorted(Comparator.comparingInt(QueueMember::getPosition))
                        .map(member -> new MemberRow(
                                member.getId(),
                                member.getClientId(),
                                member.getPosition(),
                                member.getStatus().name(),
                                member.getCreatedAt(),
                                member.getEstimatedStartTime()))
                        .toList()));
    }

    @Override
    @Transactional
    public void pauseQueue(UUID queueId) {
        queueService.pauseQueue(requireQueue(queueId).getProviderId());
    }

    @Override
    @Transactional
    public void resumeQueue(UUID queueId) {
        queueService.resumeQueue(requireQueue(queueId).getProviderId());
    }

    @Override
    @Transactional
    public void closeQueue(UUID queueId) {
        queueService.closeQueue(requireQueue(queueId).getProviderId());
    }

    @Override
    @Transactional
    public void advanceQueue(UUID queueId) {
        queueService.advanceQueue(requireQueue(queueId).getProviderId());
    }

    private Queue requireQueue(UUID queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> new NoSuchElementException("Queue not found"));
    }

    private static QueueStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return QueueStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static QueueRow toRow(Queue queue) {
        long waiting = queue.getMembers().stream()
                .filter(member -> member.getStatus() == QueueMemberStatus.WAITING
                        || member.getStatus() == QueueMemberStatus.NOTIFIED)
                .count();
        long serving = queue.getMembers().stream()
                .filter(member -> member.getStatus() == QueueMemberStatus.ACTIVE)
                .count();
        return new QueueRow(
                queue.getId(),
                queue.getProviderId(),
                queue.getStatus().name(),
                waiting,
                serving,
                waiting + serving,
                queue.getUpdatedAt());
    }
}
