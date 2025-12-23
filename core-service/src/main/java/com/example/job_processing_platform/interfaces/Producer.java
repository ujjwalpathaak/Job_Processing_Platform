package com.example.job_processing_platform.interfaces;

import com.example.job_processing_platform.enums.JobCategory;

public interface Producer<T> {
    void publish(T message, JobCategory jobCategory);
}