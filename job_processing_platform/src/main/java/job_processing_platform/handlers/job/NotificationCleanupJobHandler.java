package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationCleanupJobHandler extends AbstractJobHandler {

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.NOTIFICATION_CLEANUP;
    }

    @Override
    public JobCategory category() {
        return JobCategory.STANDARD;
    }

    @Override
    public int retries() {
        return 0;
    }

    @Override
    public List<String> backoff() {
        return List.of();
    }

    @Override
    protected void execute(JobMessage message) {
        if (message.getData() == null) {
            throw new IllegalArgumentException("Payload missing for cleanup job");
        }
        System.out.println("Cleanup completed for job " + message.getJobId());
    }
}