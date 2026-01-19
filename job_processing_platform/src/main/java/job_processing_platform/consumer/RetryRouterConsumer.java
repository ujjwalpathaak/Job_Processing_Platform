package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.service.log;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RetryRouterConsumer {
    private final RabbitProperties rabbitProperties;
    private final RabbitTemplate rabbitTemplate;

    public RetryRouterConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @RabbitListener(
            queues = "retry.route.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void route(JobMessage job, Message raw, Channel channel) throws IOException {
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

        channel.basicAck(
                raw.getMessageProperties().getDeliveryTag(),
                false
        );

        log.info("{} - ROUTED - to: {}, through: {}", job.getJobId(), routingKey, exchange);
    }
}
