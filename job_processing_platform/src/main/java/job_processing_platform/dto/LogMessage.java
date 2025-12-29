package job_processing_platform.dto;

import job_processing_platform.interfaces.Message;

import java.io.Serializable;
import java.time.Instant;

public class LogMessage implements Serializable, Message {
    private Long jobId;
    private String jobType;
    private String message;
    private String createdAt;

    public Long getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public LogMessage() {
    }

    public LogMessage(Long jobId, String jobType, String message) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.message = message;
        this.createdAt = Instant.now().toString();
    }
}
