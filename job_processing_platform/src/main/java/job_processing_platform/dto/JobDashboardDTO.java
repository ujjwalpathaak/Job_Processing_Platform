package job_processing_platform.dto;

import job_processing_platform.enums.JobCategory;
import job_processing_platform.enums.JobHandlerType;
import job_processing_platform.enums.JobStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record JobDashboardDTO(
        Long id,
        JobStatus status,
        JobCategory jobCategory,
        JobHandlerType jobHandler,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> data,
        List<JobHistoryDTO> history
) {
}
