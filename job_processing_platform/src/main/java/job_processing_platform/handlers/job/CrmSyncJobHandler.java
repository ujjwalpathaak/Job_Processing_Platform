package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrmSyncJobHandler extends AbstractJobHandler {

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.CRM_SYNC;
    }

    @Override
    public JobCategory category() {
        return JobCategory.EXTERNAL;
    }

    @Override
    public int retries() {
        return 2;
    }

    @Override
    public List<String> backoff() {
        return List.of("5s", "60s");
    }

    @Override
    protected void execute(JobMessage message) {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ignored) {
        }

        throw new RuntimeException("CRM API timeout");
    }
}