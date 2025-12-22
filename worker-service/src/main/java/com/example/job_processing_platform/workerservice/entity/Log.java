package com.example.job_processing_platform.workerservice.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "logs")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private String message;

    @Column(nullable = false, updatable = false)
    private String jobType;

    @Column(nullable = false, updatable = false)
    private Long jobId;

    protected Log() {
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getMessage() {
        return message;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public Log(Long jobId, String jobType, String message) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.message = message;
        this.createdAt = Instant.now();
    }
}