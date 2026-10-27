package ma.noba.queue.infra.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class QueueMetrics {

    private final MeterRegistry registry;
    private volatile int activeQueueCount;

    public QueueMetrics(MeterRegistry registry) {
        this.registry = registry;

        Gauge.builder("queue.active.count", () -> activeQueueCount)
                .tag("service", "noba-backend")
                .description("Number of active queues")
                .register(registry);
    }

    public void recordJoin() {
        Counter.builder("queue.join.total")
                .tag("service", "noba-backend")
                .description("Customers joined queue")
                .register(registry)
                .increment();
    }

    public void recordLeave(String reason) {
        Counter.builder("queue.leave.total")
                .tag("reason", reason)
                .tag("service", "noba-backend")
                .description("Customers left queue")
                .register(registry)
                .increment();
    }

    public void recordAdvance() {
        Counter.builder("queue.advance.total")
                .tag("service", "noba-backend")
                .description("Queue advanced to next customer")
                .register(registry)
                .increment();
    }

    public Timer.Sample startJoinTimer() {
        return Timer.start(registry);
    }

    public void stopJoinTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("queue.join.duration")
                .tag("service", "noba-backend")
                .description("Queue join operation duration")
                .register(registry));
    }

    public void recordWaitTime(double minutes) {
        DistributionSummary.builder("queue.wait.time.minutes")
                .tag("service", "noba-backend")
                .description("Customer wait time in minutes")
                .baseUnit("minutes")
                .register(registry)
                .record(minutes);
    }

    public void setActiveQueueCount(int count) {
        this.activeQueueCount = count;
    }
}
