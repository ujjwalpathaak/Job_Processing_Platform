package job_processing_platform.utils;

import job_processing_platform.interfaces.JobHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobHandlerStartupValidator {

    public JobHandlerStartupValidator(List<JobHandler> handlers) {
        handlers.forEach(JobHandlerValidator::validate);
    }
}