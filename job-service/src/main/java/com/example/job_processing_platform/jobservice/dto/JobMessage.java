package com.example.job_processing_platform.jobservice.dto;

import com.example.job_processing_platform.jobservice.entity.Job;

import java.io.Serializable;
import java.util.Map;

public class JobMessage implements Serializable {

    private Long jobId;
    private String jobType;
    private Map<String, Object> data;

    public JobMessage() {}

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public JobMessage(Job job) {
        this.jobId = job.getId();
        this.jobType = job.getType();
        this.data = job.getData();
    }
}