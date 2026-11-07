package job_processing_platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInsightsResponseDTO(
        String summary,
        String answer,
        List<String> findings,
        List<String> recommendations,
        AiInsightMetricsDTO metrics,
        AiInsightSourceDTO source,
        AiInsightContextDTO context
) {
}
