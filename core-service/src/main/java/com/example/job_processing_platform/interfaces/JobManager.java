package com.example.job_processing_platform.interfaces;

import java.time.Instant;
import java.util.Map;

public interface JobManager {

    void execute(String type, Map<String, Object> data);

    void schedule(String type, Map<String, Object> data, Instant scheduledAt);
}