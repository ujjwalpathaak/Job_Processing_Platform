package job_processing_platform.factory;

import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.handlers.job.EmailJobHandler;
import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

@Component
public class JobHandlerFactory {
    public JobHandler get(JobHandlerType handlerType) {
        return switch (handlerType) {
            case EMAIL -> new EmailJobHandler();
        };
    }
}
