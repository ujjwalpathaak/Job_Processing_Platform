package job_processing_platform.consumer;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class AbstractRetryRouterConsumer {
    protected final RabbitTemplate rabbitTemplate;
    protected final RabbitProperties rabbitProperties;

    public AbstractRetryRouterConsumer(RabbitTemplate rabbitTemplate, RabbitProperties rabbitProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    public void routeInternal(JobMessage job) {
        JobCategory category = job.getJobCategory();

        rabbitTemplate.convertAndSend(
                rabbitProperties.getRabbit().getExchanges().get(category),
                rabbitProperties.getRabbit().getRetries().get,
                job
        );
    }
}
