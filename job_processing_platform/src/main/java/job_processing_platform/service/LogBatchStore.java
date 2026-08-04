package job_processing_platform.service;

import job_processing_platform.dto.LogMessage;

import java.util.List;

public interface LogBatchStore {
    void add(String requestId, LogMessage logMessage);

    List<LogMessage> drain(String requestId);

    int size(String requestId);

    boolean isEmpty(String requestId);
}
