package job_processing_platform.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_chunks", indexes = {
        @Index(name = "idx_job_chunks_created_at", columnList = "created_at")
})
public class JobChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_name")
    private String sourceName;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected JobChunk() {
    }

    public JobChunk(Long jobId, String sourceType, String sourceName, String content) {
        this.jobId = jobId;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public String getSourceType() { return sourceType; }
    public String getSourceName() { return sourceName; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
