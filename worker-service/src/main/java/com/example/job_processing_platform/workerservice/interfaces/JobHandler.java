package com.example.job_processing_platform.workerservice.interfaces;

import com.example.job_processing_platform.workerservice.dto.ConsumerJobMessage;

public interface JobHandler<T> {
    T identify();
    void handle(ConsumerJobMessage message);
}
