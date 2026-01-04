package job_processing_platform.interfaces;

import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;

import java.util.List;

public interface JobHandler {
    String identify();

    JobCategory category();

    int retries();

    List<String> backoff();

    void process(JobMessage message);
}