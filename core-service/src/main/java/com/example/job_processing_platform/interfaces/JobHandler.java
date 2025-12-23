package com.example.job_processing_platform.interfaces;

import com.example.job_processing_platform.dto.JobMessage;

public interface JobHandler {
    JobDefinition definition();

    void handle(JobMessage message);
}
