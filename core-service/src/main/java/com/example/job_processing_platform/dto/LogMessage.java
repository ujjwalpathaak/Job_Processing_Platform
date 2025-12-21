package com.example.job_processing_platform.dto;

import java.time.Instant;

public class LogMessage {
    private Long jobId;
    private String message;
    private Instant createdAt;

    public LogMessage(Long jobId, String message) {
        this.jobId = jobId;
        this.message = message;
        this.createdAt = Instant.now();
    }
}
