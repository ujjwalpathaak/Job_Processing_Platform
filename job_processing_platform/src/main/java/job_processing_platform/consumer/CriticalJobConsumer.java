package job_processing_platform.consumer;

import com.rabbitmq.client.Channel;
import job_processing_platform.config.RabbitProperties;
import job_processing_platform.dto.JobMessage;
import job_processing_platform.factory.JobHandlerFactory;
import job_processing_platform.service.JobService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CriticalJobConsumer extends AbstractJobConsumer {

    public CriticalJobConsumer(
            RabbitTemplate rabbitTemplate,
            RabbitProperties rabbitProperties,
            JobHandlerFactory jobHandlerFactory,
            JobService jobService) {
        super(rabbitTemplate, rabbitProperties, jobHandlerFactory, jobService);
    }

    @RabbitListener(
            queues = "${job.platform.rabbit.critical.queue}",
            containerFactory = "criticalFactory"
    )
    public void consume(JobMessage job, Message raw, Channel channel)
            throws Exception {
        consumeInternal(
                job,
                raw,
                channel
        );
    }
}