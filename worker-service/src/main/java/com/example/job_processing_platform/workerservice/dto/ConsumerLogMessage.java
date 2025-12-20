package com.example.job_processing_platform.workerservice.dto;

import java.time.Instant;

public class ConsumerLogMessage {
    public Long jobId;
    public String message;
    public Instant createdAt;
}