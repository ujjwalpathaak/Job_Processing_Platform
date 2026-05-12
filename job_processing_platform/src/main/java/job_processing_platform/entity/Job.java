package job_processing_platform.entity;

import jakarta.persistence.*;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.interfaces.JobHandler;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private JobCategory jobCategory;

    @Column(nullable = false)
    private JobHandlerType jobHandler;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    protected Job() {
    }

    public Job(JobHandler handler, Map<String, Object> data) {
        this.status = JobStatus.SCHEDULED;
        this.jobCategory = handler.category();
        this.jobHandler = handler.identify();
        this.data = data;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public JobHandlerType getJobHandler() {
        return jobHandler;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getData() {
        return data;
    }
}