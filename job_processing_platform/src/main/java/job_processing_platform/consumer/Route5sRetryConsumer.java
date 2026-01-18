package job_processing_platform.consumer;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class Route5sRetryConsumer extends AbstractRetryRouterConsumer {
    public Route5sRetryConsumer(RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties) {
        super(rabbitTemplate, rabbitProperties);
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.retry}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void route(JobMessage job) {
        routeInternal(job);
    }

}
