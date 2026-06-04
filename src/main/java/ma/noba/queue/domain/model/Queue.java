package ma.noba.queue.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "queue")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Queue {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueStatus status;

    @OneToMany(mappedBy = "queue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @JsonIgnore
    private List<QueueMember> members = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public Queue(UUID providerId) {
        this.providerId = providerId;
        this.status = QueueStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Queue create(UUID providerId) {
        return new Queue(providerId);
    }

    public QueueMember addMember(UUID clientId, Duration estimatedDuration, String serviceId) {
        return addMember(clientId, estimatedDuration, serviceId, null);
    }

    public QueueMember addMember(UUID clientId, Duration estimatedDuration, String serviceId, UUID employeeId) {
        return addMember(clientId, estimatedDuration, serviceId, employeeId, null, 1);
    }

    public QueueMember addMember(UUID clientId, Duration estimatedDuration, String serviceId, UUID employeeId, UUID groupId, int groupSize) {
        return addMember(clientId, estimatedDuration, serviceId, employeeId, groupId, groupSize, QueuePriority.NORMAL);
    }

    public QueueMember addMember(UUID clientId, Duration estimatedDuration, String serviceId, UUID employeeId, UUID groupId, int groupSize, QueuePriority priority) {
        ensureActive();
        checkDuplicate(clientId);

        List<QueueMember> active = members.stream()
                .filter(QueueMember::isActive)
                .sorted(Comparator.comparingInt(QueueMember::getPosition))
                .toList();

        int insertIndex = 0;
        if (priority == QueuePriority.NORMAL) {
            insertIndex = active.size();
        } else if (priority == QueuePriority.URGENT) {
            insertIndex = (int) active.stream().filter(m -> m.getPriority() == QueuePriority.EMERGENCY).count();
        }
        // EMERGENCY: insertIndex stays 0 (ahead of everyone)

        int newPosition = insertIndex + 1;
        active.stream().skip(insertIndex).forEach(m -> m.setPosition(m.getPosition() + 1));

        Instant estimatedStart = calculateEstimatedStart(newPosition);

        QueueMember member = new QueueMember(this, clientId, newPosition, estimatedDuration, estimatedStart, serviceId, employeeId, groupId, groupSize, priority);
        members.add(member);
        recalculatePositions();
        this.updatedAt = Instant.now();
        return member;
    }

    public void removeMember(UUID memberId) {
        ensureActive();
        boolean removed = members.removeIf(m -> m.getId().equals(memberId) && m.cancel());
        if (!removed) {
            throw new IllegalArgumentException("Queue member not found or cannot be cancelled");
        }
        recalculatePositions();
        this.updatedAt = Instant.now();
    }

    public void advanceToNext() {
        ensureActive();
        members.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.ACTIVE)
                .findFirst()
                .ifPresent(QueueMember::complete);

        members.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING
                        || m.getStatus() == QueueMemberStatus.NOTIFIED)
                .findFirst()
                .ifPresent(QueueMember::activate);

        recalculatePositions();
        this.updatedAt = Instant.now();
    }

    public void startService(UUID memberId) {
        ensureActive();
        members.stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .ifPresent(QueueMember::activate);
        this.updatedAt = Instant.now();
    }

    public void pause() {
        this.status = QueueStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    public void resume() {
        this.status = QueueStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void close() {
        this.status = QueueStatus.CLOSED;
        this.updatedAt = Instant.now();
    }

    public int getWaitingCount() {
        return (int) members.stream()
                .filter(m -> m.getStatus() == QueueMemberStatus.WAITING || m.getStatus() == QueueMemberStatus.NOTIFIED)
                .count();
    }

    public Optional<QueueMember> findMember(UUID memberId) {
        return members.stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst();
    }

    public Optional<QueueMember> findMemberByClient(UUID clientId) {
        return members.stream()
                .filter(m -> m.getClientId().equals(clientId) && m.isActive())
                .findFirst();
    }

    private void ensureActive() {
        if (status != QueueStatus.ACTIVE && status != QueueStatus.PAUSED) {
            throw new IllegalStateException("Queue is not active");
        }
    }

    private void checkDuplicate(UUID clientId) {
        boolean alreadyJoined = members.stream()
                .anyMatch(m -> m.getClientId().equals(clientId) && m.isActive());
        if (alreadyJoined) {
            throw new IllegalStateException("Client already in queue");
        }
    }

    private void recalculatePositions() {
        Comparator<QueueMember> byPriority = Comparator.comparingInt(m -> m.getPriority().ordinal());
        List<QueueMember> active = members.stream()
                .filter(QueueMember::isActive)
                .sorted(byPriority.thenComparingInt(QueueMember::getPosition))
                .toList();

        Instant nextStart = Instant.now();
        for (int i = 0; i < active.size(); i++) {
            QueueMember member = active.get(i);
            member.setPosition(i + 1);
            if (member.getStatus() == QueueMemberStatus.WAITING || member.getStatus() == QueueMemberStatus.NOTIFIED) {
                member.setEstimatedStartTime(nextStart);
                nextStart = nextStart.plus(member.getEstimatedDuration());
            } else if (member.getStatus() == QueueMemberStatus.ACTIVE && member.getEstimatedStartTime() != null) {
                nextStart = nextStart.plus(member.getEstimatedDuration());
            }
        }
    }

    private Instant calculateEstimatedStart(int position) {
        Instant nextStart = Instant.now();
        for (int i = 1; i < position; i++) {
            int pos = i;
            var ahead = members.stream()
                    .filter(m -> m.getPosition() == pos && m.isActive())
                    .findFirst();
            if (ahead.isPresent()) {
                nextStart = nextStart.plus(ahead.get().getEstimatedDuration());
            }
        }
        return nextStart;
    }

    public List<QueueMember> getActiveMembers() {
        return members.stream()
                .filter(QueueMember::isActive)
                .sorted(Comparator.comparingInt(QueueMember::getPosition))
                .toList();
    }
}
