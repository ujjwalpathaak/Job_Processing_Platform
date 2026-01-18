package job_processing_platform.dto;

import job_processing_platform.enums.JobStatus;

import java.time.Instant;

public record JobHistoryDTO(
        JobStatus status,
        Instant timestamp,
        String error
) {
}
