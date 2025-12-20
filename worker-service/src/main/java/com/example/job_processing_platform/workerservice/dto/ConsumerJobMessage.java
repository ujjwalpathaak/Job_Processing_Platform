package com.example.job_processing_platform.workerservice.dto;

import java.io.Serializable;
import java.util.Map;

public class ConsumerJobMessage implements Serializable {

    public Long jobId;
    public String jobType;
    public Map<String, Object> data;
}