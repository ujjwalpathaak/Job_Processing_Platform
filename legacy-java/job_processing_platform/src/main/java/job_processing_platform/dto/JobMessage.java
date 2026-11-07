package job_processing_platform.dto;

import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;

import java.io.Serializable;
import java.util.Map;

public class JobMessage implements Serializable {

    private Long jobId;
    private JobCategory jobCategory;
    private JobHandlerType jobHandler;
    private Map<String, Object> data;

    public JobMessage() {
    }

    public JobMessage(Long jobId, JobCategory jobCategory, JobHandlerType jobHandler, Map<String, Object> data) {
        this.jobId = jobId;
        this.jobCategory = jobCategory;
        this.jobHandler = jobHandler;
        this.data = data;
    }

    public Long getJobId() {
        return jobId;
    }

    public JobCategory getJobCategory() {
        return jobCategory;
    }

    public JobHandlerType getJobHandler() {
        return jobHandler;
    }

    public Map<String, Object> getData() {
        return data;
    }
}