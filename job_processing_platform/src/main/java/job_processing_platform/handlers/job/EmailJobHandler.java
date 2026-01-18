package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailJobHandler extends AbstractJobHandler {
    //  validate
    //  beforeExecute
    //  beforeExecute
    //  execute
    //  afterExecute
    //  onFailure

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.EMAIL;
    }

    @Override
    public JobCategory category() {
        return JobCategory.STANDARD;
    }

    @Override
    public int retries() {
        return 2;
    }

    @Override
    public List<String> backoff() {
        return List.of("5s", "30s");
    }

    @Override
    protected void execute(JobMessage message) {
        throw new RuntimeException("TEST FAILURE");
    }
}