package job_processing_platform.interfaces;

import job_processing_platform.enums.JobCategory;

public interface Producer<T> {
    void publish(T message, JobCategory jobCategory);
}