package job_processing_platform.dto;

public record JobQueryOptionsDTO(
        String handler,
        String status,
        String category,
        String search,
        String sortBy,
        String sortOrder,
        Integer limit,
        Integer offset
) {
    public static JobQueryOptionsDTO empty() {
        return new JobQueryOptionsDTO(null, null, null, null, null, null, null, null);
    }
}
