package job_processing_platform.producer;

import job_processing_platform.dto.LogIngestionPayloadDTO;
import job_processing_platform.service.log;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogRagProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${job.platform.rabbit.log-rag.queue:log.rag.queue}")
    private String logRagQueue;

    public LogRagProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean publishLogForRag(LogIngestionPayloadDTO payload) {
        try {
            rabbitTemplate.convertAndSend("", logRagQueue, payload);
            return true;
        } catch (Exception ex) {
            log.error("event=rag.log.publish_failed error={}", ex.getMessage());
            return false;
        }
    }
}
