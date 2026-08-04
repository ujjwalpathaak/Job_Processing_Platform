package job_processing_platform.dto;

public record RagEmbeddingMatchDTO(
        Long id,
        String sourceType,
        String sourceName,
        String content,
        String requestId,
        Double similarity
) {
}
