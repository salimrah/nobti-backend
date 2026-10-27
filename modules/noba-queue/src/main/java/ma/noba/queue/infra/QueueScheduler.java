package ma.noba.queue.infra;

import ma.noba.queue.domain.port.service.QueueSchedulingPort;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class QueueScheduler implements QueueSchedulingPort {

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    private final Scheduler scheduler;

    @Value("${queue.notification.reminder-before-minutes:5}")
    private int reminderBeforeMinutes;

    public QueueScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void scheduleTurnReminder(UUID clientId, UUID providerId, int position, Instant reminderTime) {
        String jobKey = "reminder-" + clientId;
        String triggerKey = "reminder-trigger-" + clientId;

        try {
            scheduler.deleteJob(new JobKey(jobKey));

            JobDataMap data = new JobDataMap();
            data.put("clientId", clientId.toString());
            data.put("providerId", providerId.toString());
            data.put("position", position);

            JobDetail job = JobBuilder.newJob(TurnReminderJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(data)
                    .storeDurably(true)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .startAt(java.util.Date.from(reminderTime))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(job, trigger);
            log.info("Scheduled turn reminder for client {} at {}", clientId, reminderTime);
        } catch (SchedulerException e) {
            log.error("Failed to schedule turn reminder for client {}", clientId, e);
        }
    }

    public void cancelTurnReminder(UUID clientId) {
        String jobKey = "reminder-" + clientId;
        try {
            scheduler.deleteJob(new JobKey(jobKey));
            log.info("Cancelled turn reminder for client {}", clientId);
        } catch (SchedulerException e) {
            log.error("Failed to cancel turn reminder for client {}", clientId, e);
        }
    }

    public void rescheduleTurnReminder(UUID clientId, UUID providerId, int position, Instant estimatedStartTime) {
        Instant reminderTime = estimatedStartTime.minus(reminderBeforeMinutes, ChronoUnit.MINUTES);

        if (reminderTime.isAfter(Instant.now())) {
            scheduleTurnReminder(clientId, providerId, position, reminderTime);
            log.info("Rescheduled turn reminder for client {} at {}", clientId, reminderTime);
        } else {
            cancelTurnReminder(clientId);
            log.info("Cancelled reminder for client {} — estimated start too soon", clientId);
        }
    }
}
