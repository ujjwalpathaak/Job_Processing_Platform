package job_processing_platform.handlers;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.interfaces.JobDefinition;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

@Component
public class EmailHandler implements JobHandler {
    @Override
    public JobDefinition definition() {
        return new JobDefinition() {
            public String identify() {
                return "EMAIL_HANDLER";
            }

            public JobCategory category() {
                return JobCategory.STANDARD;
            }
        };
    }

    @Override
    public void handle(JobMessage message) {
        System.out.println("Email Sent");
    }
}
