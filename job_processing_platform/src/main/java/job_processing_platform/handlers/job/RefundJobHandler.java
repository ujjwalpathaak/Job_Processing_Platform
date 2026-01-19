package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundJobHandler extends AbstractJobHandler {

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.REFUND;
    }

    @Override
    public JobCategory category() {
        return JobCategory.CRITICAL;
    }

    @Override
    public int retries() {
        return 3;
    }

    @Override
    public List<String> backoff() {
        return List.of("5s", "30s", "60s");
    }

    @Override
    protected void execute(JobMessage message) {
        if (Math.random() < 0.3) {
            throw new RuntimeException("Duplicate refund detected");
        }
        System.out.println("Refund completed");
    }
}