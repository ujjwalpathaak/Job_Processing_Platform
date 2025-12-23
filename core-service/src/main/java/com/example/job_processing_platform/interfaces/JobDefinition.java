package com.example.job_processing_platform.interfaces;

import com.example.job_processing_platform.enums.JobCategory;

import java.time.Duration;

public interface JobDefinition {
    String identify();              // email, pdf, search

    JobCategory category();        // FAST, SLOW, CRITICAL

    int maxRetries();              // 3

    Duration retryDelay();         // 10s
}