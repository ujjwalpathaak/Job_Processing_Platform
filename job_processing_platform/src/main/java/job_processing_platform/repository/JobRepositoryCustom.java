package job_processing_platform.repository;

import job_processing_platform.dto.JobQueryOptionsDTO;
import job_processing_platform.entity.Job;

import java.time.Instant;
import java.util.List;

public interface JobRepositoryCustom {

    List<Job> findWithOptions(JobQueryOptionsDTO options, Instant since);

    long countWithOptions(JobQueryOptionsDTO options, Instant since);
}
