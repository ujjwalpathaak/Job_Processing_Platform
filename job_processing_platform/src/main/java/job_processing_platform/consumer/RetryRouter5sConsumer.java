package job_processing_platform.consumer;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RetryRouter5sConsumer extends AbstractRetryRouterConsumer {

    public RetryRouter5sConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties
    ) {
        super(rabbitTemplate, rabbitProperties);
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.retries.5s.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void route(JobMessage job) {
        routeInternal(job);
    }
}
