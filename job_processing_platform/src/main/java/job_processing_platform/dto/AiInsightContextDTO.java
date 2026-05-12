package job_processing_platform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInsightContextDTO(
        List<AiInsightContextJobDTO> failedJobs,
        List<String> recentErrors,
        List<String> sampleLogs,
        List<AiRetrievedChunkDTO> retrievedChunks
) {
}
