package ma.noba.queue.domain.model;

/** Lifecycle of a waitlist entry. */
public enum QueueWaitlistStatus {
    /** Waiting to be told the queue is open / has room. */
    WAITING,
    /** The client has been notified and can now join. */
    NOTIFIED,
    /** The client joined the queue (or otherwise converted). */
    CONVERTED,
    /** The client left the waitlist. */
    CANCELLED
}
