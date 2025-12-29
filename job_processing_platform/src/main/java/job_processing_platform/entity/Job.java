package job_processing_platform.entity;

import jakarta.persistence.*;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobStatus;
import job_processing_platform.interfaces.JobHandler;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private JobCategory jobCategory;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    protected Job() {
    }

    public Job(JobHandler handler, Map<String, Object> data) {
        this.status = JobStatus.SCHEDULED;
        this.jobCategory = handler.definition().category();
        this.type = handler.definition().identify();
        this.data = data;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Map<String, Object> getData() {
        return data;
    }
}