package job_processing_platform.interfaces;

import job_processing_platform.enums.JobCategory;

public interface JobDefinition {
    String identify();

    JobCategory category();
}