package job_processing_platform.dto;

import java.util.List;

public record PaginatedJobsDTO(
        List<JobDashboardDTO> items,
        DashboardJobsMetaDTO meta
) {
}
