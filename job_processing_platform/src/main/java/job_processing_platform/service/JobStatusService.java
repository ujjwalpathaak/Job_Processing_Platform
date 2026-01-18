package job_processing_platform.service;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import job_processing_platform.entity.Job;
import job_processing_platform.entity.JobStateHistory;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.repository.JobRepository;
import job_processing_platform.repository.JobStatusHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class JobStatusService {

    private final JobRepository jobRepository;
    private final JobStatusHistoryRepository historyRepository;

    public JobStatusService(
            JobRepository jobRepository,
            JobStatusHistoryRepository historyRepository
    ) {
        this.jobRepository = jobRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void updateJobStatus(
            Job job,
            JobStatus newStatus,
            @Nullable String errorMessage
    ) {
        if (newStatus == null) {
            throw new IllegalArgumentException("JobStatus cannot be null");
        }

        int updated = jobRepository.updateJobStatus(job.getId(), newStatus);
        if (updated == 0) {
            throw new EntityNotFoundException("Job not found: " + job.getId());
        }

        historyRepository.save(
                new JobStateHistory(job.getId(), newStatus, errorMessage)
        );
    }
}
