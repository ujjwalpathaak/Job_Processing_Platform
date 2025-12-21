package com.example.job_processing_platform.dto;

import java.io.Serializable;
import java.util.Map;

public class JobMessage implements Serializable {

    private Long jobId;
    private String jobType;
    private Map<String, Object> data;

    public JobMessage() {
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public JobMessage(Long jobId, String jobType, Map<String, Object> data) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.data = data;
    }
}