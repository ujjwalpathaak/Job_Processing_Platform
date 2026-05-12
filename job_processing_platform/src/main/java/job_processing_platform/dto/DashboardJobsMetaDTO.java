package job_processing_platform.dto;

public record DashboardJobsMetaDTO(
        long total,
        int page,
        int limit,
        int totalPages,
        boolean hasMore
) {
}
