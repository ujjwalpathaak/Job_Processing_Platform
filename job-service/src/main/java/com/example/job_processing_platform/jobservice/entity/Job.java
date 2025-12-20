package com.example.job_processing_platform.jobservice.entity;

import com.example.job_processing_platform.jobservice.enums.JobStatus;
import jakarta.persistence.*;
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

    // null means -> instant trigger
    private Instant scheduledAt;

    @Column(nullable = false)
    private JobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    protected Job() {
    }

    public Job(String type, Map<String, Object> data) {
        this.status = JobStatus.SCHEDULED;
        this.scheduledAt = null;
        this.type = type;
        this.data = data;
        this.createdAt = Instant.now();
    }

    public Job(String type, Map<String, Object> data, Instant scheduledAt) {
        this.status = JobStatus.SCHEDULED;
        this.scheduledAt = scheduledAt;
        this.type = type;
        this.data = data;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Map<String, Object> getData() {
        return data;
    }
}