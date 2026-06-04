package ma.noba.shared.domain.port;

public interface AdminQueuePort {
    long countActiveQueues();
    long countTotalQueueMembers();
}
