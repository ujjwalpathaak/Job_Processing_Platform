package job_processing_platform.dto;

public record AiRetrievedChunkDTO(
        String id,
        String sourceType,
        String sourceName,
        String content,
        int score
) {
}
