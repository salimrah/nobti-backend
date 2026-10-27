package ma.noba.queue;

import ma.noba.queue.domain.model.Queue;
import ma.noba.queue.domain.model.QueueMember;
import ma.noba.queue.domain.model.QueueMemberStatus;
import ma.noba.queue.domain.model.QueuePriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-domain regression tests for queue position assignment. These guard against the priority
 * inversion and duplicate-position bugs that previously caused 23505 constraint violations.
 */
class QueuePositionTest {

    private final UUID providerId = UUID.randomUUID();

    private QueueMember add(Queue queue, QueuePriority priority) {
        return queue.addMember(UUID.randomUUID(), Duration.ofMinutes(15), null, null, null, 1, priority);
    }

    @Test
    @DisplayName("Emergency members are positioned ahead of urgent and normal members")
    void higherPriorityIsPositionedFirst() {
        Queue queue = Queue.create(providerId);
        add(queue, QueuePriority.NORMAL);
        add(queue, QueuePriority.URGENT);
        add(queue, QueuePriority.EMERGENCY);

        var active = queue.getActiveMembers();

        assertThat(active).extracting(QueueMember::getPriority)
                .containsExactly(QueuePriority.EMERGENCY, QueuePriority.URGENT, QueuePriority.NORMAL);
        assertThat(active).extracting(QueueMember::getPosition).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("When the serving client leaves, the next waiting client is promoted")
    void leavingServingMemberPromotesNext() {
        Queue queue = Queue.create(providerId);
        var first = queue.addMember(UUID.randomUUID(), Duration.ofMinutes(15), null);
        var second = queue.addMember(UUID.randomUUID(), Duration.ofMinutes(15), null);
        first.setId(UUID.randomUUID());
        second.setId(UUID.randomUUID());

        queue.advanceToNext(); // activates the first (serving)
        assertThat(queue.getActiveMembers()).extracting(QueueMember::getStatus)
                .contains(QueueMemberStatus.ACTIVE);

        var promoted = queue.removeMember(first.getId()); // the serving client leaves

        assertThat(promoted).isPresent();
        assertThat(promoted.get().getId()).isEqualTo(second.getId());
        assertThat(queue.getActiveMembers()).extracting(QueueMember::getStatus)
                .contains(QueueMemberStatus.ACTIVE);
        assertThat(queue.getActiveMembers()).extracting(QueueMember::getId).contains(second.getId());
    }

    @Test
    @DisplayName("Positions stay unique across all members after advancing the queue")
    void positionsRemainUniqueAfterAdvance() {
        Queue queue = Queue.create(providerId);
        add(queue, QueuePriority.NORMAL);
        add(queue, QueuePriority.NORMAL);

        queue.advanceToNext(); // activate the first waiting member
        queue.advanceToNext(); // complete it and activate the next one

        assertThat(queue.getMembers())
                .extracting(QueueMember::getPosition)
                .doesNotHaveDuplicates();
    }
}
