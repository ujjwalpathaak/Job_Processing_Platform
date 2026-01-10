package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.utils.JobHandlerRegistry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CriticalJobConsumer extends AbstractJobConsumer {

    public CriticalJobConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties,
            JobHandlerRegistry jobHandlerRegistry
    ) {
        super(rabbitTemplate, rabbitProperties, jobHandlerRegistry);
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.critical.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(JobMessage job, Message raw, Channel channel)
            throws Exception {

        consumeInternal(
                job,
                raw,
                channel,
                rabbitProperties.getRabbit().getCritical(),
                rabbitProperties.getRabbit().getExchanges().get(JobCategory.CRITICAL)
        );
    }
}