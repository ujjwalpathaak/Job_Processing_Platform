package job_processing_platform.dto;

import java.time.Instant;

public record LogIngestionPayloadDTO(
        String level,
        String message,
        String event,
        String error,
        Instant timestamp,
        String requestId
) {
}
