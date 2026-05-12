package job_processing_platform.dto;

import java.util.List;
import java.util.Map;

public record AiInsightMetricsDTO(
        int jobsAnalyzed,
        int logsAnalyzed,
        Map<String, Integer> jobsByStatus,
        Map<String, Integer> jobsByHandler,
        Map<String, Integer> jobsByCategory,
        Map<String, Integer> logLevels,
        List<AiInsightMetricsTopEntryDTO> topEvents,
        List<AiInsightMetricsErrorEntryDTO> topErrors
) {
}
