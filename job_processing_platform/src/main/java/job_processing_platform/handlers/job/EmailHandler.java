package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailHandler extends AbstractJobHandler {
    //  validate
    //  beforeExecute
    //  beforeExecute
    //  execute
    //  afterExecute
    //  onFailure

    @Override
    public String identify() {
        return "EMAIL_HANDLER";
    }

    @Override
    public JobCategory category() {
        return JobCategory.STANDARD;
    }

    @Override
    public int retries() {
        return 3;
    }

    @Override
    public List<String> backoff() {
        return List.of("10s", "4s", "2s");
    }

    @Override
    protected void execute(JobMessage message) {
        throw new RuntimeException("TEST FAILURE");
        // actual email logic
    }
}