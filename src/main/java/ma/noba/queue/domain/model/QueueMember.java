package ma.noba.queue.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "queue_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"queue_id", "position"})
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QueueMember {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    @JsonIgnore
    private Queue queue;

    @Column(nullable = false)
    private UUID clientId;

    private String serviceId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "group_size", nullable = false)
    private int groupSize = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueuePriority priority = QueuePriority.NORMAL;

    @Column(nullable = false)
    private int position;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(nullable = false, columnDefinition = "INTERVAL")
    private Duration estimatedDuration;

    private Instant estimatedStartTime;

    private Instant actualStartTime;

    private Instant completedTime;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(columnDefinition = "INTERVAL")
    private Duration actualDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QueueMemberStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public QueueMember(Queue queue, UUID clientId, int position, Duration estimatedDuration, Instant estimatedStartTime, String serviceId, UUID employeeId) {
        this(queue, clientId, position, estimatedDuration, estimatedStartTime, serviceId, employeeId, null, 1, QueuePriority.NORMAL);
    }

    public QueueMember(Queue queue, UUID clientId, int position, Duration estimatedDuration, Instant estimatedStartTime, String serviceId, UUID employeeId, UUID groupId, int groupSize) {
        this(queue, clientId, position, estimatedDuration, estimatedStartTime, serviceId, employeeId, groupId, groupSize, QueuePriority.NORMAL);
    }

    public QueueMember(Queue queue, UUID clientId, int position, Duration estimatedDuration, Instant estimatedStartTime, String serviceId, UUID employeeId, UUID groupId, int groupSize, QueuePriority priority) {
        this.queue = queue;
        this.clientId = clientId;
        this.position = position;
        this.estimatedDuration = estimatedDuration;
        this.estimatedStartTime = estimatedStartTime;
        this.serviceId = serviceId;
        this.employeeId = employeeId;
        this.groupId = groupId;
        this.groupSize = groupSize;
        this.priority = priority;
        this.status = QueueMemberStatus.WAITING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = QueueMemberStatus.ACTIVE;
        this.actualStartTime = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void notifyClient() {
        if (this.status == QueueMemberStatus.WAITING) {
            this.status = QueueMemberStatus.NOTIFIED;
            this.updatedAt = Instant.now();
        }
    }

    public void complete() {
        this.status = QueueMemberStatus.COMPLETED;
        this.completedTime = Instant.now();
        if (this.actualStartTime != null) {
            this.actualDuration = Duration.between(this.actualStartTime, this.completedTime);
        }
        this.updatedAt = Instant.now();
    }

    public boolean cancel() {
        if (this.status == QueueMemberStatus.COMPLETED) {
            return false;
        }
        this.status = QueueMemberStatus.CANCELLED;
        this.updatedAt = Instant.now();
        return true;
    }

    public boolean isActive() {
        return status == QueueMemberStatus.WAITING
                || status == QueueMemberStatus.NOTIFIED
                || status == QueueMemberStatus.ACTIVE;
    }
}
