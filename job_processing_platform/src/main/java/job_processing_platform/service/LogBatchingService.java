package job_processing_platform.service;

import job_processing_platform.dto.LogIngestionPayloadDTO;
import job_processing_platform.dto.LogMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class LogBatchingService {

    private final LogBatchStore logBatchStore;
    private final LogRagProducerPort ragProducer;
    private final int batchSize;

    public LogBatchingService(LogBatchStore logBatchStore, LogRagProducerPort ragProducer) {
        this(logBatchStore, ragProducer, 10);
    }

    public LogBatchingService(LogBatchStore logBatchStore, LogRagProducerPort ragProducer, int batchSize) {
        this.logBatchStore = Objects.requireNonNull(logBatchStore, "logBatchStore");
        this.ragProducer = Objects.requireNonNull(ragProducer, "ragProducer");
        this.batchSize = Math.max(1, batchSize);
    }

    public void addLog(LogMessage logMessage) {
        if (logMessage == null) {
            return;
        }

        String requestId = logMessage.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        logBatchStore.add(requestId, logMessage);
        if (logBatchStore.size(requestId) >= batchSize) {
            flush(requestId);
        }
    }

    public void flush(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        List<LogMessage> batch = logBatchStore.drain(requestId);
        if (batch.isEmpty()) {
            return;
        }

        List<LogIngestionPayloadDTO> payloads = batch.stream()
                .map(this::toPayload)
                .toList();

        ragProducer.publishBatchForRag(requestId, payloads);
    }

    private LogIngestionPayloadDTO toPayload(LogMessage logMessage) {
        return new LogIngestionPayloadDTO(
                logMessage.getLevel().name(),
                logMessage.getMessage(),
                logMessage.getEvent(),
                logMessage.getError(),
                logMessage.getTimestamp(),
                logMessage.getRequestId()
        );
    }
}
