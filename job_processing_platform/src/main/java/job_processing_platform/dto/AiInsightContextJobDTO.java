package job_processing_platform.dto;

public record AiInsightContextJobDTO(
        String id,
        String handler,
        String category,
        String status,
        String updatedAt,
        String error
) {
}
