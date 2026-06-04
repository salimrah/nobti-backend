package ma.noba.shared.infra.jpa;

import ma.noba.shared.domain.model.OutboxEvent;
import ma.noba.shared.domain.port.OutboxEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOutboxEventRepository extends JpaRepository<OutboxEvent, UUID>, OutboxEventRepository {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status IN :statuses ORDER BY o.createdAt ASC")
    List<OutboxEvent> findByStatusInOrderByCreatedAtAsc(@Param("statuses") List<OutboxEvent.OutboxEventStatus> statuses, Pageable pageable);

    default List<OutboxEvent> findPending(int limit) {
        return findByStatusInOrderByCreatedAtAsc(
                List.of(OutboxEvent.OutboxEventStatus.PENDING, OutboxEvent.OutboxEventStatus.FAILED),
                Pageable.ofSize(limit));
    }
}
