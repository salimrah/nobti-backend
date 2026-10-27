package ma.noba.queue.infra;

import ma.noba.queue.domain.port.service.QueueNotificationPort;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TurnReminderJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TurnReminderJob.class);

    private final QueueNotificationPort notificationPort;

    public TurnReminderJob(QueueNotificationPort notificationPort) {
        this.notificationPort = notificationPort;
    }

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap data = context.getJobDetail().getJobDataMap();
        UUID clientId = UUID.fromString(data.getString("clientId"));
        UUID providerId = UUID.fromString(data.getString("providerId"));
        int position = data.getInt("position");

        log.info("Firing turn reminder for client {} at provider {}, position {}", clientId, providerId, position);

        try {
            notificationPort.notifyEstimatedWait(clientId, providerId, position);
        } catch (Exception e) {
            log.error("Failed to send turn reminder for client {}", clientId, e);
        }
    }
}
