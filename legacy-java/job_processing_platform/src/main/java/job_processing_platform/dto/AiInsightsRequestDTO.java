package job_processing_platform.dto;

public record AiInsightsRequestDTO(
        Integer hours,
        Integer logLines,
        Integer jobLimit,
        String question,
        Boolean includeRawContext
) {
}
