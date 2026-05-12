package job_processing_platform.dto;

import java.util.List;

public record AiInsightSourceDTO(
        String generatedAt,
        boolean usedAiModel,
        String model,
        int hours,
        int logLinesRequested,
        int jobLimitRequested,
        List<String> logFiles,
        boolean retrievalEnabled,
        int retrievedChunks
) {
}
