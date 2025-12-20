package com.example.job_processing_platform.workerservice.dto;

import com.example.job_processing_platform.workerservice.enums.JobType;

import java.io.Serializable;
import java.util.Map;

public class ConsumerJobMessage implements Serializable {

    private Long id;
    private JobType type;
    private Map<String, Object> data;

    public Long getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }
}