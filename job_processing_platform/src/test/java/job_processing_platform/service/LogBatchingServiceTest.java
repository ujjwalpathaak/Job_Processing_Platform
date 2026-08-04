package job_processing_platform.service;

import job_processing_platform.dto.LogIngestionPayloadDTO;
import job_processing_platform.dto.LogMessage;
import job_processing_platform.enums.LogLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogBatchingServiceTest {

    @Test
    void shouldFlushBatchWhenThresholdReached() {
        InMemoryLogBatchStore store = new InMemoryLogBatchStore();
        RecordingRagProducer producer = new RecordingRagProducer();
        LogBatchingService service = new LogBatchingService(store, producer, 10);

        for (int i = 0; i < 10; i++) {
            service.addLog(new LogMessage("message-" + i, LogLevel.INFO, "req-123"));
        }

        assertEquals(1, producer.batches.size());
        assertEquals(10, producer.batches.get(0).logs().size());
        assertEquals("req-123", producer.batches.get(0).requestId());
        assertTrue(store.isEmpty("req-123"));
    }

    private static class InMemoryLogBatchStore implements LogBatchStore {
        private final java.util.Map<String, List<LogMessage>> batches = new java.util.LinkedHashMap<>();

        @Override
        public void add(String requestId, LogMessage logMessage) {
            batches.computeIfAbsent(requestId, ignored -> new ArrayList<>()).add(logMessage);
        }

        @Override
        public List<LogMessage> drain(String requestId) {
            List<LogMessage> batch = batches.remove(requestId);
            return batch != null ? batch : List.of();
        }

        @Override
        public int size(String requestId) {
            return batches.getOrDefault(requestId, List.of()).size();
        }

        @Override
        public boolean isEmpty(String requestId) {
            return size(requestId) == 0;
        }
    }

    private static class RecordingRagProducer implements LogRagProducerPort {
        private final List<LogBatchPayload> batches = new ArrayList<>();

        @Override
        public void publishBatchForRag(String requestId, List<LogIngestionPayloadDTO> payloads) {
            batches.add(new LogBatchPayload(requestId, payloads));
        }
    }

    private record LogBatchPayload(String requestId, List<LogIngestionPayloadDTO> logs) {
    }
}
