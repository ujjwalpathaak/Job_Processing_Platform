package job_processing_platform.consumer;

import job_processing_platform.dto.LogIngestionPayloadDTO;
import job_processing_platform.service.RagEmbeddingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LogRagConsumer {

    private final RagEmbeddingService ragEmbeddingService;

    public LogRagConsumer(RagEmbeddingService ragEmbeddingService) {
        this.ragEmbeddingService = ragEmbeddingService;
    }

    @RabbitListener(queues = "${job.platform.rabbit.log-rag.queue:log.rag.queue}")
    public void consume(LogIngestionPayloadDTO payload) {
        if (payload == null) {
            return;
        }

        String text = String.join("\n",
                payload.message() != null ? payload.message() : "",
                payload.event() != null ? payload.event() : "",
                payload.error() != null ? payload.error() : ""
        ).trim();

        if (text.isBlank()) {
            return;
        }

        String embedding = ragEmbeddingService.buildEmbedding(text);
        ragEmbeddingService.saveEmbedding(
                "log",
                payload.event() != null ? payload.event() : payload.level(),
                text,
                payload.requestId(),
                embedding
        );
    }
}
