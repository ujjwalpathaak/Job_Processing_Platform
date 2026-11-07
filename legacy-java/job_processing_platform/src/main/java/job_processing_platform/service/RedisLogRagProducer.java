package job_processing_platform.service;

import job_processing_platform.dto.LogIngestionPayloadDTO;
import job_processing_platform.producer.LogRagProducer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisLogRagProducer implements LogRagProducerPort {

    private final LogRagProducer logRagProducer;

    public RedisLogRagProducer(LogRagProducer logRagProducer) {
        this.logRagProducer = logRagProducer;
    }

    @Override
    public void publishBatchForRag(String requestId, List<LogIngestionPayloadDTO> payloads) {
        if (requestId == null || requestId.isBlank() || payloads == null || payloads.isEmpty()) {
            return;
        }

        for (LogIngestionPayloadDTO payload : payloads) {
            logRagProducer.publishLogForRag(payload);
        }
    }
}
