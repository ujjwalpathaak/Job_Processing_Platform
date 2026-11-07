package job_processing_platform.consumer;

import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.service.log;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public abstract class AbstractRetryRouterConsumer {

    protected final RabbitTemplate rabbitTemplate;
    protected final RabbitProperties rabbitProperties;

    protected AbstractRetryRouterConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    protected void routeInternal(JobMessage job, Message raw, String backoff) {
        JobCategory category = job.getJobCategory();

        String exchange =
                rabbitProperties.getRabbit()
                        .getExchanges()
                        .get(category);

        String routingKey =
                rabbitProperties.getRabbit()
                        .getQueue(category)
                        .getRoutingKey();

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                raw
        );

        log.info("{} - ROUTED - from: {}, to: {}, through: {}", job.getJobId(), backoff, routingKey, exchange);
    }
}
