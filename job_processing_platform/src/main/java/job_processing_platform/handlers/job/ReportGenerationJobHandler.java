package job_processing_platform.handlers.job;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportGenerationJobHandler extends AbstractJobHandler {

    @Override
    public JobHandlerType identify() {
        return JobHandlerType.REPORT_GENERATION;
    }

    @Override
    public JobCategory category() {
        return JobCategory.STANDARD;
    }

    @Override
    public int retries() {
        return 1;
    }

    @Override
    public List<String> backoff() {
        return List.of("30s");
    }

    @Override
    protected void execute(JobMessage message) {
        try {
            // simulate heavy processing
            Thread.sleep(3000);
            System.out.println("Report generated for job " + message.getJobId());
        } catch (InterruptedException e) {
            throw new RuntimeException("Report generation interrupted", e);
        }
    }
}