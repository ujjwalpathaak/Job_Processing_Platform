package job_processing_platform.service;

import job_processing_platform.dto.LogIngestionPayloadDTO;

import java.util.List;

public interface LogRagProducerPort {
    void publishBatchForRag(String requestId, List<LogIngestionPayloadDTO> payloads);
}
