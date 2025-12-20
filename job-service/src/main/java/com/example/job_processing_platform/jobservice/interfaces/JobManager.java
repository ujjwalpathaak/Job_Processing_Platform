package com.example.job_processing_platform.jobservice.interfaces;

import com.example.job_processing_platform.jobservice.entity.Job;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface JobManager {

    void execute(String type, Map<String, Object> data);
    void schedule(String type, Map<String, Object> data, Instant scheduledAt);
}