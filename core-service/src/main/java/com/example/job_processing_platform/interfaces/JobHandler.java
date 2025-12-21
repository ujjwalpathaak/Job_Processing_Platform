package com.example.job_processing_platform.interfaces;

import com.example.job_processing_platform.dto.JobMessage;

public interface JobHandler<T> {
    T identify();

    void handle(JobMessage message);
}
