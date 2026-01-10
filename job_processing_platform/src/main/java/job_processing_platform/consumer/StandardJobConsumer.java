package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.Rabbit.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.enums.JobCategory;
import job_processing_platform.registry.JobHandlerRegistry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class StandardJobConsumer extends AbstractJobConsumer {

    public StandardJobConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties,
            JobHandlerRegistry jobHandlerRegistry
    ) {
        super(rabbitTemplate, rabbitProperties, jobHandlerRegistry);
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.standard.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consume(JobMessage job, Message raw, Channel channel)
            throws Exception {

        consumeInternal(
                job,
                raw,
                channel,
                rabbitProperties.getRabbit().getStandard(),
                rabbitProperties.getRabbit().getExchanges().get(JobCategory.STANDARD)
        );
    }
}