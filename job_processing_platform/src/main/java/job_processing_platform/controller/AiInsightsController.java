package job_processing_platform.controller;

import job_processing_platform.dto.AiInsightsRequestDTO;
import job_processing_platform.dto.AiInsightsResponseDTO;
import job_processing_platform.dto.ApiResponse;
import job_processing_platform.service.AiInsightsService;
import job_processing_platform.service.log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;

    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }

    @PostMapping("/insights")
    public ResponseEntity<ApiResponse<AiInsightsResponseDTO>> getInsights(
            @RequestBody(required = false) AiInsightsRequestDTO request
    ) {
        log.info("event=api.insights.request_received hours={}, logLines={}, jobLimit={}, hasQuestion={}",
                request != null ? request.hours() : null,
                request != null ? request.logLines() : null,
                request != null ? request.jobLimit() : null,
                request != null && request.question() != null
        );

        AiInsightsResponseDTO response = aiInsightsService.generateAiInsights(request);

        log.info("event=api.insights.generated usedAiModel={}, retrievedChunks={}",
                response.source().usedAiModel(),
                response.source().retrievedChunks()
        );

        return ResponseEntity.ok(ApiResponse.success("Insights generated successfully", response));
    }
}
