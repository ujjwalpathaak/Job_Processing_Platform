package job_processing_platform.interfaces;

import job_processing_platform.dto.JobMessage;

public interface JobHandler {
    JobDefinition definition();

    void handle(JobMessage message);
}
