package job_processing_platform.entity;

import jakarta.persistence.*;
import job_processing_platform.enums.JobStatus;

import java.time.Instant;

@Entity
@Table(name = "job_state_history",
        indexes = {
                @Index(name = "idx_job_id", columnList = "jobId"),
                @Index(name = "idx_job_status", columnList = "status")
        })
public class JobStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(columnDefinition = "text")
    private String errorMessage;

    protected JobStateHistory() {
    }

    public JobStateHistory(
            Long jobId,
            JobStatus status,
            String errorMessage
    ) {
        this.jobId = jobId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = Instant.now();
    }

    public Long getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
