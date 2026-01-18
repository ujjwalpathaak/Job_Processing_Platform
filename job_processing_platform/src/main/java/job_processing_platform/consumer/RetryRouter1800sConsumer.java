package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RetryRouter1800sConsumer extends AbstractRetryRouterConsumer {

    public RetryRouter1800sConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties
    ) {
        super(rabbitTemplate, rabbitProperties);
    }

    @RabbitListener(
            queues = "retry.1800s.route.queue",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void route(JobMessage job, Message raw,
                      Channel channel) throws IOException {
        routeInternal(job, raw, "1800s");

        channel.basicAck(
                raw.getMessageProperties().getDeliveryTag(),
                false
        );
    }
}
