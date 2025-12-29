package job_processing_platform.dto;

import job_processing_platform.interfaces.Message;

import java.io.Serializable;
import java.util.Map;

public class JobMessage implements Serializable, Message {

    private Long jobId;
    private String jobType;
    private Map<String, Object> data;

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public JobMessage() {
    }

    public JobMessage(Long jobId, String jobType, Map<String, Object> data) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.data = data;
    }
}